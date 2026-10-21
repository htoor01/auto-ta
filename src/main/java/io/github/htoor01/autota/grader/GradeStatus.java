package io.github.htoor01.autota.grader;

/** How grading one submission ended. Everything except {@code GRADED} needs the instructor's attention. */
public enum GradeStatus {
    /** Tests ran; the suggested grade is the share of tests passed. */
    GRADED,
    /** The submission (or the tests against it) did not compile. Suggested grade 0. */
    COMPILE_ERROR,
    /** The container hit the wall-clock limit. Suggested grade 0. */
    TIMEOUT,
    /** No Java files were attached. No suggested grade. */
    NO_SUBMISSION,
    /** AutoTA or Docker failed; says nothing about the student's work. No suggested grade. */
    ERROR
}
