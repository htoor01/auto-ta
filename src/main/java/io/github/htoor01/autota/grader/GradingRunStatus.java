package io.github.htoor01.autota.grader;

/** Where a background grading run is. Runs still QUEUED or RUNNING when AutoTA stops become FAILED on restart. */
public enum GradingRunStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED
}
