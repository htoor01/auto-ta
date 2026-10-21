package io.github.htoor01.autota.grader;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class DockerContainerRunnerTest {

    private final DockerContainerRunner runner = new DockerContainerRunner(new GraderProperties(
            "docker", "autota-grader:latest", Path.of("grader-tests"), Path.of("grader-work"),
            Duration.ofSeconds(60), "512m", 1.0, 128));

    @Test
    void locksDownTheContainer() {
        List<String> command = runner.command("autota-grade-1", Path.of("/w/sub"), Path.of("/w/tests"), Path.of("/w/rep"));

        assertThat(String.join(" ", command)).contains(
                "--network none",
                "--memory 512m",
                "--memory-swap 512m",
                "--cpus 1.0",
                "--pids-limit 128",
                "--read-only",
                "--cap-drop ALL",
                "--security-opt no-new-privileges",
                "--label autota.grader=true",
                "--rm");
        assertThat(command.getLast()).isEqualTo("autota-grader:latest");
    }

    @Test
    void onlyTheReportsFolderIsWritable() {
        List<String> command = runner.command("autota-grade-1", Path.of("/w/sub"), Path.of("/w/tests"), Path.of("/w/rep"));

        assertThat(command).contains(
                "type=bind,src=/w/sub,dst=/work/submission,readonly",
                "type=bind,src=/w/tests,dst=/work/tests,readonly",
                "type=bind,src=/w/rep,dst=/reports");
    }
}
