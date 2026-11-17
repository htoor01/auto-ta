package io.github.htoor01.autota.grader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import io.github.htoor01.autota.grader.GradingService.GradingJob;

/**
 * Accepts grading requests and runs them one at a time in the background, saving each result as it
 * finishes. One worker keeps a single container running and a single SQLite writer at any moment.
 */
@Service
public class GradingQueue {

    private static final Logger log = LoggerFactory.getLogger(GradingQueue.class);

    private final GradingService gradingService;
    private final GradingRunRepository repository;
    private final TaskExecutor executor;

    GradingQueue(GradingService gradingService, GradingRunRepository repository,
                 @Qualifier(GraderConfig.EXECUTOR) TaskExecutor executor) {
        this.gradingService = gradingService;
        this.repository = repository;
        this.executor = executor;
        // Runs the previous process didn't finish will never finish now. Done before any request is accepted.
        int interrupted = repository.failUnfinished("AutoTA stopped before this run finished; grade again");
        if (interrupted > 0) {
            log.warn("Marked {} unfinished grading run(s) from a previous start as FAILED", interrupted);
        }
        int leftovers = gradingService.removeLeftoverWorkDirs();
        if (leftovers > 0) {
            log.warn("Removed {} grading work folder(s) left behind by a previous start", leftovers);
        }
    }

    /** Validates the assignment now (404 if wrong), then queues it. Returns the QUEUED run. */
    public GradingRun submit(String courseId, String courseWorkId) {
        GradingJob job = gradingService.prepare(courseId, courseWorkId);
        GradingRun run = repository.create(courseId, courseWorkId);
        try {
            executor.execute(() -> process(run.id(), job));
        } catch (TaskRejectedException e) {
            repository.finish(run.id(), GradingRunStatus.FAILED, "Grading queue was full");
            throw new GradingQueueFullException("Too many grading runs are waiting; try again shortly");
        }
        return run;
    }

    private void process(String runId, GradingJob job) {
        repository.markRunning(runId);
        try {
            gradingService.grade(job, result -> repository.saveResult(runId, result));
            repository.finish(runId, GradingRunStatus.COMPLETED, null);
        } catch (RuntimeException e) {
            // e.g. Classroom unreachable while listing submissions. Per-submission failures don't land here.
            log.error("Grading run {} failed", runId, e);
            repository.finish(runId, GradingRunStatus.FAILED, e.getMessage());
        }
    }
}
