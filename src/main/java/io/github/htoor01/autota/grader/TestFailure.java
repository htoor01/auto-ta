package io.github.htoor01.autota.grader;

/** One failed instructor test, e.g. {@code subtractsInOrder()} with "expected: <6> but was: <-6>". */
public record TestFailure(String test, String message) {
}
