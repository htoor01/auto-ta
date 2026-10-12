package io.github.htoor01.autota.classroom.google;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.google.api.services.classroom.model.AssignmentSubmission;
import com.google.api.services.classroom.model.Date;
import com.google.api.services.classroom.model.StudentSubmission;

import io.github.htoor01.autota.classroom.model.Attachment;
import io.github.htoor01.autota.classroom.model.Course;
import io.github.htoor01.autota.classroom.model.CourseWork;
import io.github.htoor01.autota.classroom.model.Submission;

/** Converts Google's Classroom API objects into AutoTA's own model records. */
final class GoogleModelMapper {

    private GoogleModelMapper() {
    }

    static Course toCourse(com.google.api.services.classroom.model.Course course) {
        return new Course(course.getId(), course.getName(), course.getSection(), course.getCourseState());
    }

    static CourseWork toCourseWork(com.google.api.services.classroom.model.CourseWork courseWork) {
        return new CourseWork(
                courseWork.getId(),
                courseWork.getCourseId(),
                courseWork.getTitle(),
                courseWork.getMaxPoints(),
                toLocalDate(courseWork.getDueDate()),
                courseWork.getState());
    }

    static Submission toSubmission(StudentSubmission submission, String studentName) {
        return new Submission(
                submission.getId(),
                submission.getCourseId(),
                submission.getCourseWorkId(),
                submission.getUserId(),
                studentName,
                submission.getState(),
                Boolean.TRUE.equals(submission.getLate()),
                submission.getDraftGrade(),
                submission.getAssignedGrade(),
                toAttachments(submission.getAssignmentSubmission()));
    }

    /** Keeps only Drive file attachments; links, YouTube videos and forms can't be graded. */
    private static List<Attachment> toAttachments(AssignmentSubmission assignmentSubmission) {
        if (assignmentSubmission == null || assignmentSubmission.getAttachments() == null) {
            return List.of();
        }
        return assignmentSubmission.getAttachments().stream()
                .map(com.google.api.services.classroom.model.Attachment::getDriveFile)
                .filter(Objects::nonNull)
                .map(file -> new Attachment(file.getId(), file.getTitle()))
                .toList();
    }

    /** Classroom dates have separate year/month/day fields, any of which may be missing. */
    private static LocalDate toLocalDate(Date date) {
        if (date == null || date.getYear() == null || date.getMonth() == null || date.getDay() == null) {
            return null;
        }
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }
}
