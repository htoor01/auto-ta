package io.github.htoor01.autota.classroom;

/** A course, coursework, submission or file does not exist (or is not visible to this user). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
