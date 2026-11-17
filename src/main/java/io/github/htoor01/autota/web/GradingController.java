package io.github.htoor01.autota.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.htoor01.autota.classroom.ResourceNotFoundException;
import io.github.htoor01.autota.grader.GradingQueue;
import io.github.htoor01.autota.grader.GradingRun;
import io.github.htoor01.autota.grader.GradingRunRepository;

/**
 * Starts grading runs and reads their results. Results are suggestions for the instructor to approve;
 * nothing is sent to Classroom.
 */
@RestController
class GradingController {

    private static final int RECENT_RUNS = 20;

    private final GradingQueue gradingQueue;
    private final GradingRunRepository repository;

    GradingController(GradingQueue gradingQueue, GradingRunRepository repository) {
        this.gradingQueue = gradingQueue;
        this.repository = repository;
    }

    /** Returns 202 straight away; poll the run's Location until its status is COMPLETED or FAILED. */
    @PostMapping("/api/courses/{courseId}/coursework/{courseWorkId}/grade")
    ResponseEntity<GradingRun> grade(@PathVariable String courseId, @PathVariable String courseWorkId) {
        GradingRun run = gradingQueue.submit(courseId, courseWorkId);
        return ResponseEntity.accepted().location(URI.create("/api/grading-runs/" + run.id())).body(run);
    }

    @GetMapping("/api/grading-runs/{runId}")
    GradingRun run(@PathVariable String runId) {
        return repository.find(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Grading run " + runId + " not found"));
    }

    /** The most recent runs for an assignment, newest first. */
    @GetMapping("/api/courses/{courseId}/coursework/{courseWorkId}/grading-runs")
    List<GradingRun> runs(@PathVariable String courseId, @PathVariable String courseWorkId) {
        return repository.findByCourseWork(courseId, courseWorkId, RECENT_RUNS);
    }
}
