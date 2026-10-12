package io.github.htoor01.autota.classroom;

import java.util.List;

import io.github.htoor01.autota.classroom.model.AttachmentContent;
import io.github.htoor01.autota.classroom.model.Course;
import io.github.htoor01.autota.classroom.model.CourseWork;
import io.github.htoor01.autota.classroom.model.Submission;

/**
 * Everything AutoTA needs from Google Classroom. The rest of the app depends only on this
 * interface, so the real Google client and the mock client are interchangeable via Spring profiles.
 *
 * <p>Implementations throw {@link ResourceNotFoundException} for unknown IDs and
 * {@link ClassroomException} for any other failure talking to Classroom.
 */
public interface ClassroomClient {

    /** Active courses the signed-in user teaches. */
    List<Course> listCourses();

    List<CourseWork> listCourseWork(String courseId);

    List<Submission> listSubmissions(String courseId, String courseWorkId);

    /** Downloads a submitted file, e.g. a student's {@code .java} source. */
    AttachmentContent downloadAttachment(String driveFileId);

    /**
     * Sets a submission's grade. A draft grade is only visible to teachers; otherwise the grade is
     * also set as the assigned grade. Students see an assigned grade once the submission is returned.
     */
    void pushGrade(String courseId, String courseWorkId, String submissionId, double grade, boolean draftOnly);
}
