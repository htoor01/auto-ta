package io.github.htoor01.autota.classroom.google;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import com.google.api.services.classroom.model.AssignmentSubmission;
import com.google.api.services.classroom.model.Attachment;
import com.google.api.services.classroom.model.Date;
import com.google.api.services.classroom.model.DriveFile;
import com.google.api.services.classroom.model.Link;
import com.google.api.services.classroom.model.StudentSubmission;
import org.junit.jupiter.api.Test;

import io.github.htoor01.autota.classroom.model.CourseWork;
import io.github.htoor01.autota.classroom.model.Submission;

class GoogleModelMapperTest {

    @Test
    void mapsCourseWorkDueDate() {
        CourseWork work = GoogleModelMapper.toCourseWork(new com.google.api.services.classroom.model.CourseWork()
                .setId("cw-1")
                .setCourseId("course-1")
                .setTitle("Calculator")
                .setMaxPoints(100.0)
                .setDueDate(new Date().setYear(2026).setMonth(10).setDay(20))
                .setState("PUBLISHED"));

        assertThat(work).isEqualTo(new CourseWork("cw-1", "course-1", "Calculator", 100.0,
                LocalDate.of(2026, 10, 20), "PUBLISHED"));
    }

    @Test
    void incompleteDueDateBecomesNull() {
        CourseWork work = GoogleModelMapper.toCourseWork(new com.google.api.services.classroom.model.CourseWork()
                .setId("cw-1")
                .setDueDate(new Date().setYear(2026).setMonth(10)));

        assertThat(work.dueDate()).isNull();
        assertThat(work.maxPoints()).isNull();
    }

    @Test
    void mapsSubmissionKeepingOnlyDriveFileAttachments() {
        StudentSubmission googleSubmission = new StudentSubmission()
                .setId("sub-1")
                .setCourseId("course-1")
                .setCourseWorkId("cw-1")
                .setUserId("user-1")
                .setState("TURNED_IN")
                .setLate(true)
                .setDraftGrade(90.0)
                .setAssignmentSubmission(new AssignmentSubmission().setAttachments(List.of(
                        new Attachment().setDriveFile(new DriveFile().setId("drive-1").setTitle("Calculator.java")),
                        new Attachment().setLink(new Link().setUrl("https://example.com")))));

        Submission submission = GoogleModelMapper.toSubmission(googleSubmission, "Student A");

        assertThat(submission.studentName()).isEqualTo("Student A");
        assertThat(submission.late()).isTrue();
        assertThat(submission.draftGrade()).isEqualTo(90.0);
        assertThat(submission.assignedGrade()).isNull();
        assertThat(submission.attachments())
                .containsExactly(new io.github.htoor01.autota.classroom.model.Attachment("drive-1", "Calculator.java"));
    }

    @Test
    void submissionWithoutAttachmentsOrLateFlag() {
        Submission submission = GoogleModelMapper.toSubmission(new StudentSubmission().setId("sub-1"), "Student A");

        assertThat(submission.late()).isFalse();
        assertThat(submission.attachments()).isEmpty();
    }
}
