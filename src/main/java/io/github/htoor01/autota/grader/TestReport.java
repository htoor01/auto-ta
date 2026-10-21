package io.github.htoor01.autota.grader;

import java.util.List;

/** Totals from a JUnit XML report. Errors (exceptions, per-test timeouts) count as failures. */
record TestReport(int total, List<TestFailure> failures) {

    TestReport {
        failures = List.copyOf(failures);
    }

    int passed() {
        return total - failures.size();
    }
}
