package io.github.htoor01.autota.grader;

import java.time.Instant;
import java.util.List;

/**
 * One grading run of an assignment and the results saved so far. While the run is RUNNING,
 * {@code results} grows as each submission finishes.
 *
 * @param error why the run FAILED (individual submission failures are results with status ERROR instead)
 */
public record GradingRun(
        String id,
        String courseId,
        String courseWorkId,
        GradingRunStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String error,
        List<GradingResult> results) {

    public GradingRun {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
