package io.github.htoor01.autota.grader;

import java.util.List;

/**
 * The grade AutoTA suggests for one submission. It is only a suggestion: the instructor approves it.
 *
 * @param suggestedGrade points out of {@code maxPoints}; null when there is nothing to suggest
 *                       ({@code NO_SUBMISSION}, {@code ERROR}, or an ungraded assignment)
 * @param detail         compiler output or the error, for statuses other than {@code GRADED}
 */
public record GradingResult(
        String submissionId,
        String studentId,
        String studentName,
        GradeStatus status,
        int testsPassed,
        int testsTotal,
        Double suggestedGrade,
        Double maxPoints,
        List<TestFailure> failures,
        long durationMillis,
        String detail) {

    public GradingResult {
        failures = failures == null ? List.of() : List.copyOf(failures);
    }
}
