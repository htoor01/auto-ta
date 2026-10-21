package io.github.htoor01.autota.grader;

import java.io.IOException;
import java.nio.file.Path;

/** Runs instructor tests against one submission in isolation. Swapped for a fake in tests. */
interface ContainerRunner {

    /**
     * @param submissionDir student's {@code .java} files (mounted read-only)
     * @param testsDir      instructor tests (mounted read-only)
     * @param reportsDir    where the JUnit XML report is written (the only writable mount)
     */
    Run run(Path submissionDir, Path testsDir, Path reportsDir) throws IOException, InterruptedException;

    /** Exit codes come from {@code docker/grader/grade.sh}. */
    record Run(int exitCode, boolean timedOut, String output) {

        static final int COMPILE_ERROR = 3;
    }
}
