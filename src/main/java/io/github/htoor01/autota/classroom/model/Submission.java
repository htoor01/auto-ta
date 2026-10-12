package io.github.htoor01.autota.classroom.model;

import java.util.List;

/**
 * One student's submission for an assignment. {@code state} is Classroom's submission state,
 * e.g. {@code TURNED_IN} or {@code RETURNED}. Grades are null until set.
 */
public record Submission(
        String id,
        String courseId,
        String courseWorkId,
        String studentId,
        String studentName,
        String state,
        boolean late,
        Double draftGrade,
        Double assignedGrade,
        List<Attachment> attachments) {

    public Submission {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
