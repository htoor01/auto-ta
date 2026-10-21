package io.github.htoor01.autota.grader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.htoor01.autota.classroom.ClassroomClient;
import io.github.htoor01.autota.classroom.ResourceNotFoundException;
import io.github.htoor01.autota.classroom.model.Attachment;
import io.github.htoor01.autota.classroom.model.AttachmentContent;
import io.github.htoor01.autota.classroom.model.CourseWork;
import io.github.htoor01.autota.classroom.model.Submission;

/** Grading logic with a fake container runner, so Docker isn't needed. */
class GradingServiceTest {

    @TempDir
    private Path dir;

    private final ClassroomClient classroomClient = mock(ClassroomClient.class);
    private ContainerRunner.Run nextRun;
    private boolean writeReport;

    private GradingService service;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(dir.resolve("tests/cw-1"));
        GraderProperties properties = new GraderProperties("docker", "img", dir.resolve("tests"), dir.resolve("work"),
                Duration.ofSeconds(60), "512m", 1.0, 128);
        ContainerRunner fakeRunner = (submissionDir, testsDir, reportsDir) -> {
            assertThat(submissionDir.resolve("Calculator.java")).exists();
            if (writeReport) {
                Files.copy(JUnitReportParserTest.sampleReport(), reportsDir.resolve(JUnitReportParser.REPORT_FILE));
            }
            return nextRun;
        };
        service = new GradingService(classroomClient, fakeRunner, properties);

        when(classroomClient.listCourseWork("c-1")).thenReturn(List.of(
                new CourseWork("cw-1", "c-1", "Calculator", 90.0, LocalDate.of(2026, 10, 20), "PUBLISHED")));
        when(classroomClient.downloadAttachment("f-1"))
                .thenReturn(new AttachmentContent("Calculator.java", "class Calculator {}".getBytes()));
    }

    @Test
    void scoresShareOfTestsPassed() {
        givenSubmission(new Attachment("f-1", "Calculator.java"));
        writeReport = true;
        nextRun = new ContainerRunner.Run(1, false, "");

        GradingResult result = gradeOne();

        assertThat(result.status()).isEqualTo(GradeStatus.GRADED);
        assertThat(result.testsPassed()).isEqualTo(1);
        assertThat(result.testsTotal()).isEqualTo(3);
        assertThat(result.suggestedGrade()).isEqualTo(30.0);
        assertThat(result.failures()).hasSize(2);
    }

    @Test
    void compileErrorScoresZeroAndKeepsCompilerOutput() {
        givenSubmission(new Attachment("f-1", "Calculator.java"));
        nextRun = new ContainerRunner.Run(ContainerRunner.Run.COMPILE_ERROR, false, "Calculator.java:5: error: ';' expected");

        GradingResult result = gradeOne();

        assertThat(result.status()).isEqualTo(GradeStatus.COMPILE_ERROR);
        assertThat(result.suggestedGrade()).isEqualTo(0.0);
        assertThat(result.detail()).contains("';' expected");
    }

    @Test
    void timeoutScoresZero() {
        givenSubmission(new Attachment("f-1", "Calculator.java"));
        nextRun = new ContainerRunner.Run(-1, true, "");

        GradingResult result = gradeOne();

        assertThat(result.status()).isEqualTo(GradeStatus.TIMEOUT);
        assertThat(result.suggestedGrade()).isEqualTo(0.0);
    }

    @Test
    void missingReportIsAnErrorWithoutAGrade() {
        givenSubmission(new Attachment("f-1", "Calculator.java"));
        nextRun = new ContainerRunner.Run(125, false, "docker: unable to find image");

        GradingResult result = gradeOne();

        assertThat(result.status()).isEqualTo(GradeStatus.ERROR);
        assertThat(result.suggestedGrade()).isNull();
    }

    @Test
    void submissionWithoutJavaFilesIsNotRun() {
        givenSubmission(new Attachment("f-2", "notes.pdf"), new Attachment("f-3", "../../Evil.java"));

        GradingResult result = gradeOne();

        assertThat(result.status()).isEqualTo(GradeStatus.NO_SUBMISSION);
        assertThat(result.suggestedGrade()).isNull();
        verify(classroomClient, never()).downloadAttachment("f-3");
    }

    @Test
    void cleansUpWorkFolder() throws IOException {
        givenSubmission(new Attachment("f-1", "Calculator.java"));
        nextRun = new ContainerRunner.Run(ContainerRunner.Run.COMPILE_ERROR, false, "");

        gradeOne();

        try (var leftovers = Files.list(dir.resolve("work"))) {
            assertThat(leftovers).isEmpty();
        }
    }

    @Test
    void refusesAssignmentWithoutTests() {
        when(classroomClient.listCourseWork("c-1")).thenReturn(List.of(
                new CourseWork("cw-2", "c-1", "Strings", 50.0, null, "DRAFT")));

        assertThatThrownBy(() -> service.gradeAssignment("c-1", "cw-2"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No grader tests");
    }

    @Test
    void scoreIsRoundedAndNullWithoutPoints() {
        TestReport twoOfThree = new TestReport(3, List.of(new TestFailure("t", "m")));

        assertThat(GradingService.score(twoOfThree, 100.0)).isEqualTo(66.67);
        assertThat(GradingService.score(twoOfThree, null)).isNull();
    }

    private void givenSubmission(Attachment... attachments) {
        when(classroomClient.listSubmissions("c-1", "cw-1")).thenReturn(List.of(new Submission(
                "s-1", "c-1", "cw-1", "student-1", "Student A", "TURNED_IN", false, null, null, List.of(attachments))));
    }

    private GradingResult gradeOne() {
        List<GradingResult> results = service.gradeAssignment("c-1", "cw-1");
        assertThat(results).hasSize(1);
        return results.getFirst();
    }
}
