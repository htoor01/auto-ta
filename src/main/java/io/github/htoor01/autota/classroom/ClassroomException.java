package io.github.htoor01.autota.classroom;

/**
 * A call to Google Classroom failed.
 *
 * @see #getUpstreamStatus()
 */
public class ClassroomException extends RuntimeException {

    private final int upstreamStatus;

    public ClassroomException(String message, int upstreamStatus, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
    }

    /** HTTP status Google returned, or 0 if the request never got a response (e.g. network error). */
    public int getUpstreamStatus() {
        return upstreamStatus;
    }
}
