package io.github.htoor01.autota.grader;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for grading, bound from {@code autota.grader.*}.
 *
 * @param dockerCommand Docker CLI to run (a full path if the app's PATH doesn't include it)
 * @param image         grader image built from {@code docker/grader}
 * @param testsDir      instructor tests, one folder per coursework ID
 * @param workDir       scratch space for submissions and reports; must be visible to the Docker daemon
 *                      (with Colima only the home folder is shared, so the system temp folder won't work)
 * @param timeout       wall-clock limit for one container, including compiling
 * @param memory        container memory limit, e.g. {@code 512m}; swap is disabled
 * @param cpus          container CPU limit
 * @param pidsLimit     maximum processes/threads in the container (stops fork bombs)
 */
@ConfigurationProperties("autota.grader")
public record GraderProperties(
        String dockerCommand,
        String image,
        Path testsDir,
        Path workDir,
        Duration timeout,
        String memory,
        double cpus,
        int pidsLimit) {
}
