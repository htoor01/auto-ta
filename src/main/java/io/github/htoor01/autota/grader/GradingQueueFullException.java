package io.github.htoor01.autota.grader;

/** Too many grading runs are already waiting; the client should retry later (HTTP 503). */
public class GradingQueueFullException extends RuntimeException {

    public GradingQueueFullException(String message) {
        super(message);
    }
}
