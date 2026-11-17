package io.github.htoor01.autota.grader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import tools.jackson.databind.json.JsonMapper;

import io.github.htoor01.autota.classroom.ClassroomException;
import io.github.htoor01.autota.grader.GradingService.GradingJob;

/** The queue with a real SQLite database; grading itself is faked and runs on the calling thread. */
class GradingQueueTest {

    @TempDir
    private Path dir;

    private final GradingService gradingService = mock(GradingService.class);
    private final GradingJob job = new GradingJob("c-1", "cw-1", 100.0, Path.of("grader-tests/cw-1"));
    private GradingRunRepository repository;

    @BeforeEach
    void setUp() {
        repository = new GradingRunRepository(TestDatabase.create(dir), JsonMapper.builder().build());
        when(gradingService.prepare("c-1", "cw-1")).thenReturn(job);
    }

    @Test
    void gradesInTheBackgroundAndSavesEachResult() {
        doAnswer(invocation -> {
            Consumer<GradingResult> onResult = invocation.getArgument(1);
            onResult.accept(result("s-1"));
            onResult.accept(result("s-2"));
            return null;
        }).when(gradingService).grade(eq(job), any());

        GradingRun submitted = queue(Runnable::run).submit("c-1", "cw-1");

        assertThat(submitted.status()).isEqualTo(GradingRunStatus.QUEUED);
        GradingRun run = repository.find(submitted.id()).orElseThrow();
        assertThat(run.status()).isEqualTo(GradingRunStatus.COMPLETED);
        assertThat(run.results()).extracting(GradingResult::submissionId).containsExactly("s-1", "s-2");
    }

    @Test
    void runFailsWhenGradingCannotContinue() {
        doThrow(new ClassroomException("Classroom unavailable", 503, null)).when(gradingService).grade(eq(job), any());

        String runId = queue(Runnable::run).submit("c-1", "cw-1").id();

        GradingRun run = repository.find(runId).orElseThrow();
        assertThat(run.status()).isEqualTo(GradingRunStatus.FAILED);
        assertThat(run.error()).contains("Classroom unavailable");
    }

    @Test
    void fullQueueIsRejectedAndRecorded() {
        TaskExecutor full = task -> {
            throw new TaskRejectedException("full");
        };

        assertThatThrownBy(() -> queue(full).submit("c-1", "cw-1")).isInstanceOf(GradingQueueFullException.class);
        assertThat(repository.findByCourseWork("c-1", "cw-1", 20))
                .extracting(GradingRun::status).containsExactly(GradingRunStatus.FAILED);
    }

    @Test
    void startupFailsRunsTheLastProcessLeftBehind() {
        String leftOver = repository.create("c-1", "cw-1").id();
        repository.markRunning(leftOver);

        queue(Runnable::run);

        GradingRun run = repository.find(leftOver).orElseThrow();
        assertThat(run.status()).isEqualTo(GradingRunStatus.FAILED);
        assertThat(run.error()).contains("grade again");
    }

    private GradingQueue queue(TaskExecutor executor) {
        return new GradingQueue(gradingService, repository, executor);
    }

    private static GradingResult result(String submissionId) {
        return new GradingResult(submissionId, "student", "Student A", GradeStatus.GRADED, 7, 7, 100.0, 100.0,
                List.of(), 10, null);
    }
}
