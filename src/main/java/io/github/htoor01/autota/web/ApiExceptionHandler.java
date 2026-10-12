package io.github.htoor01.autota.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.htoor01.autota.classroom.ClassroomException;
import io.github.htoor01.autota.classroom.ResourceNotFoundException;

/** Turns Classroom failures into RFC 9457 problem responses with a sensible status code. */
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * Permission errors are passed through as 403. Anything else Google got wrong is a 502: the
     * request to AutoTA was fine, but the service it depends on failed.
     */
    @ExceptionHandler(ClassroomException.class)
    ProblemDetail handleClassroomFailure(ClassroomException e) {
        log.warn("Classroom request failed (upstream status {})", e.getUpstreamStatus(), e);
        HttpStatus status = e.getUpstreamStatus() == 403 ? HttpStatus.FORBIDDEN : HttpStatus.BAD_GATEWAY;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setProperty("upstreamStatus", e.getUpstreamStatus());
        return problem;
    }
}
