package io.github.htoor01.autota.grader;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

class GradingRunRepositoryTest {

    @TempDir
    private Path dir;

    private GradingRunRepository repository;

    @BeforeEach
    void setUp() {
        repository = new GradingRunRepository(TestDatabase.create(dir), JsonMapper.builder().build());
    }

    @Test
    void savesRunLifecycleAndResults() {
        GradingRun created = repository.create("c-1", "cw-1");
        repository.markRunning(created.id());
        repository.saveResult(created.id(), new GradingResult("s-1", "student-1", "Student A", GradeStatus.GRADED,
                5, 7, 71.43, 100.0, List.of(new TestFailure("subtractsInOrder()", "expected: <6> but was: <-6>")),
                850, null));
        repository.saveResult(created.id(), new GradingResult("s-2", "student-2", "Student B", GradeStatus.NO_SUBMISSION,
                0, 0, null, 100.0, List.of(), 0, "No .java files attached"));
        repository.finish(created.id(), GradingRunStatus.COMPLETED, null);

        GradingRun run = repository.find(created.id()).orElseThrow();

        assertThat(run.status()).isEqualTo(GradingRunStatus.COMPLETED);
        assertThat(run.startedAt()).isNotNull();
        assertThat(run.finishedAt()).isAfterOrEqualTo(run.startedAt());
        assertThat(run.results()).extracting(GradingResult::submissionId).containsExactly("s-1", "s-2");
        assertThat(run.results().getFirst().failures())
                .containsExactly(new TestFailure("subtractsInOrder()", "expected: <6> but was: <-6>"));
        assertThat(run.results().getFirst().suggestedGrade()).isEqualTo(71.43);
        assertThat(run.results().get(1).suggestedGrade()).isNull();
    }

    @Test
    void failsRunsLeftUnfinishedByAPreviousStart() {
        String queued = repository.create("c-1", "cw-1").id();
        String running = repository.create("c-1", "cw-1").id();
        repository.markRunning(running);
        String done = repository.create("c-1", "cw-1").id();
        repository.finish(done, GradingRunStatus.COMPLETED, null);

        assertThat(repository.failUnfinished("restarted")).isEqualTo(2);

        assertThat(repository.find(queued).orElseThrow().status()).isEqualTo(GradingRunStatus.FAILED);
        assertThat(repository.find(running).orElseThrow().error()).isEqualTo("restarted");
        assertThat(repository.find(done).orElseThrow().status()).isEqualTo(GradingRunStatus.COMPLETED);
    }

    @Test
    void listsRunsForOneAssignmentNewestFirst() throws InterruptedException {
        String older = repository.create("c-1", "cw-1").id();
        Thread.sleep(5);
        String newer = repository.create("c-1", "cw-1").id();
        repository.create("c-1", "cw-other");

        assertThat(repository.findByCourseWork("c-1", "cw-1", 20))
                .extracting(GradingRun::id).containsExactly(newer, older);
        assertThat(repository.find("missing")).isEmpty();
    }
}
