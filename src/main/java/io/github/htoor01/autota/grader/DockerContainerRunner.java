package io.github.htoor01.autota.grader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Runs each submission in its own throwaway container from the grader image.
 *
 * <p>Student code is untrusted, so the container has no network, no Linux capabilities, a read-only
 * filesystem, runs as {@code nobody}, and has memory, CPU and process limits. Every container is
 * labelled so ones left behind by a crash can be found and removed.
 */
@Component
class DockerContainerRunner implements ContainerRunner {

    static final String LABEL = "autota.grader=true";
    private static final Logger log = LoggerFactory.getLogger(DockerContainerRunner.class);
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;

    private final GraderProperties properties;

    DockerContainerRunner(GraderProperties properties) {
        this.properties = properties;
    }

    @Override
    public Run run(Path submissionDir, Path testsDir, Path reportsDir) throws IOException, InterruptedException {
        String name = "autota-grade-" + UUID.randomUUID();
        Process process = new ProcessBuilder(command(name, submissionDir, testsDir, reportsDir))
                .redirectErrorStream(true)
                .start();
        // Read output on another thread so a chatty submission can't block on a full pipe.
        BoundedOutput output = new BoundedOutput(process.getInputStream());
        Thread reader = Thread.ofVirtual().start(output);

        boolean finished = process.waitFor(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            // Killing the docker CLI does not stop the container, so remove it by name.
            removeContainers(List.of(name));
            process.destroyForcibly();
        }
        reader.join(TimeUnit.SECONDS.toMillis(5));
        return new Run(finished ? process.exitValue() : -1, !finished, output.text());
    }

    /** The full {@code docker run} command; package-private so the sandbox flags can be tested. */
    List<String> command(String name, Path submissionDir, Path testsDir, Path reportsDir) {
        return List.of(
                properties.dockerCommand(), "run", "--rm",
                "--name", name,
                "--label", LABEL,
                "--network", "none",
                "--memory", properties.memory(),
                "--memory-swap", properties.memory(),
                "--cpus", String.valueOf(properties.cpus()),
                "--pids-limit", String.valueOf(properties.pidsLimit()),
                "--read-only",
                "--tmpfs", "/tmp:rw,size=64m",
                "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges",
                "--mount", bind(submissionDir, "/work/submission", true),
                "--mount", bind(testsDir, "/work/tests", true),
                "--mount", bind(reportsDir, "/reports", false),
                properties.image());
    }

    /** Removes containers left over from a previous run that crashed or was killed mid-grade. */
    @EventListener(ApplicationReadyEvent.class)
    void removeOrphans() {
        try {
            Process process = new ProcessBuilder(properties.dockerCommand(), "ps", "-aq", "--filter", "label=" + LABEL)
                    .redirectErrorStream(true)
                    .start();
            String ids = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.waitFor() != 0) {
                log.warn("Docker is not available ({}); grading will fail until it is", ids);
            } else if (!ids.isEmpty()) {
                List<String> orphans = List.of(ids.split("\\s+"));
                log.warn("Removing {} orphaned grader container(s)", orphans.size());
                removeContainers(orphans);
            }
        } catch (IOException e) {
            log.warn("Docker is not available ({}); grading will fail until it is", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void removeContainers(List<String> namesOrIds) {
        try {
            List<String> command = new ArrayList<>(List.of(properties.dockerCommand(), "rm", "-f"));
            command.addAll(namesOrIds);
            new ProcessBuilder(command).redirectErrorStream(true).start().waitFor(30, TimeUnit.SECONDS);
        } catch (IOException e) {
            log.error("Could not remove grader container(s) {}", namesOrIds, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String bind(Path source, String target, boolean readOnly) {
        return "type=bind,src=" + source.toAbsolutePath() + ",dst=" + target + (readOnly ? ",readonly" : "");
    }

    /** Keeps the first {@value #MAX_OUTPUT_BYTES} bytes of output and discards the rest. */
    private static final class BoundedOutput implements Runnable {

        private final InputStream in;
        private final ByteArrayOutputStream kept = new ByteArrayOutputStream();

        BoundedOutput(InputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[8192];
            try (in) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    synchronized (kept) {
                        kept.write(buffer, 0, Math.max(0, Math.min(read, MAX_OUTPUT_BYTES - kept.size())));
                    }
                }
            } catch (IOException ignored) {
                // The process was killed; keep what was read.
            }
        }

        String text() {
            synchronized (kept) {
                return kept.toString(StandardCharsets.UTF_8);
            }
        }
    }
}
