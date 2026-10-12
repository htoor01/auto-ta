package io.github.htoor01.autota.classroom.google;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.ListCourseWorkResponse;
import com.google.api.services.classroom.model.ListCoursesResponse;
import com.google.api.services.classroom.model.ListStudentSubmissionsResponse;
import com.google.api.services.classroom.model.ListStudentsResponse;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.StudentSubmission;
import com.google.api.services.drive.Drive;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.github.htoor01.autota.classroom.ClassroomClient;
import io.github.htoor01.autota.classroom.ClassroomException;
import io.github.htoor01.autota.classroom.ResourceNotFoundException;
import io.github.htoor01.autota.classroom.model.AttachmentContent;
import io.github.htoor01.autota.classroom.model.Course;
import io.github.htoor01.autota.classroom.model.CourseWork;
import io.github.htoor01.autota.classroom.model.Submission;

/** {@link ClassroomClient} backed by the real Google Classroom and Drive APIs. */
@Component
@Profile("google")
class GoogleClassroomClient implements ClassroomClient {

    private final Classroom classroom;
    private final Drive drive;

    GoogleClassroomClient(Classroom classroom, Drive drive) {
        this.classroom = classroom;
        this.drive = drive;
    }

    @Override
    public List<Course> listCourses() {
        List<Course> courses = new ArrayList<>();
        String pageToken = null;
        do {
            String token = pageToken;
            ListCoursesResponse response = call("list courses", () -> classroom.courses().list()
                    .setTeacherId("me")
                    .setCourseStates(List.of("ACTIVE"))
                    .setPageToken(token)
                    .execute());
            if (response.getCourses() != null) {
                response.getCourses().forEach(course -> courses.add(GoogleModelMapper.toCourse(course)));
            }
            pageToken = response.getNextPageToken();
        } while (pageToken != null);
        return courses;
    }

    @Override
    public List<CourseWork> listCourseWork(String courseId) {
        List<CourseWork> courseWork = new ArrayList<>();
        String pageToken = null;
        do {
            String token = pageToken;
            ListCourseWorkResponse response = call("list coursework for course " + courseId,
                    () -> classroom.courses().courseWork().list(courseId)
                            .setPageToken(token)
                            .execute());
            if (response.getCourseWork() != null) {
                response.getCourseWork().forEach(work -> courseWork.add(GoogleModelMapper.toCourseWork(work)));
            }
            pageToken = response.getNextPageToken();
        } while (pageToken != null);
        return courseWork;
    }

    @Override
    public List<Submission> listSubmissions(String courseId, String courseWorkId) {
        Map<String, String> studentNames = studentNamesById(courseId);
        List<Submission> submissions = new ArrayList<>();
        String pageToken = null;
        do {
            String token = pageToken;
            ListStudentSubmissionsResponse response = call("list submissions for coursework " + courseWorkId,
                    () -> classroom.courses().courseWork().studentSubmissions().list(courseId, courseWorkId)
                            .setPageToken(token)
                            .execute());
            if (response.getStudentSubmissions() != null) {
                for (StudentSubmission submission : response.getStudentSubmissions()) {
                    String name = studentNames.getOrDefault(submission.getUserId(), "Unknown student");
                    submissions.add(GoogleModelMapper.toSubmission(submission, name));
                }
            }
            pageToken = response.getNextPageToken();
        } while (pageToken != null);
        return submissions;
    }

    @Override
    public AttachmentContent downloadAttachment(String driveFileId) {
        String fileName = call("look up file " + driveFileId,
                () -> drive.files().get(driveFileId).setFields("name").execute().getName());
        byte[] content = call("download file " + driveFileId, () -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            drive.files().get(driveFileId).executeMediaAndDownloadTo(out);
            return out.toByteArray();
        });
        return new AttachmentContent(fileName, content);
    }

    @Override
    public void pushGrade(String courseId, String courseWorkId, String submissionId, double grade,
                          boolean draftOnly) {
        StudentSubmission update = new StudentSubmission().setDraftGrade(grade);
        String updateMask = "draftGrade";
        if (!draftOnly) {
            update.setAssignedGrade(grade);
            updateMask = "draftGrade,assignedGrade";
        }
        String mask = updateMask;
        call("update grade for submission " + submissionId,
                () -> classroom.courses().courseWork().studentSubmissions()
                        .patch(courseId, courseWorkId, submissionId, update)
                        .setUpdateMask(mask)
                        .execute());
    }

    private Map<String, String> studentNamesById(String courseId) {
        Map<String, String> names = new HashMap<>();
        String pageToken = null;
        do {
            String token = pageToken;
            ListStudentsResponse response = call("list students for course " + courseId,
                    () -> classroom.courses().students().list(courseId)
                            .setPageToken(token)
                            .execute());
            if (response.getStudents() != null) {
                for (Student student : response.getStudents()) {
                    if (student.getProfile() != null && student.getProfile().getName() != null) {
                        names.put(student.getUserId(), student.getProfile().getName().getFullName());
                    }
                }
            }
            pageToken = response.getNextPageToken();
        } while (pageToken != null);
        return names;
    }

    @FunctionalInterface
    private interface GoogleCall<T> {
        T execute() throws IOException;
    }

    /** Runs a Google API request, translating its checked exceptions into AutoTA's exceptions. */
    private static <T> T call(String action, GoogleCall<T> request) {
        try {
            return request.execute();
        } catch (GoogleJsonResponseException e) {
            int status = e.getStatusCode();
            String detail = e.getDetails() != null ? e.getDetails().getMessage() : e.getStatusMessage();
            if (status == 404) {
                throw new ResourceNotFoundException("Could not " + action + ": not found");
            }
            if (status == 403 && detail != null && detail.contains("ProjectPermissionDenied")) {
                throw new ClassroomException("Could not " + action + ": Classroom only lets an app modify"
                        + " coursework created by the same Google Cloud project. Create the assignment"
                        + " through the API with this project's credentials.", status, e);
            }
            throw new ClassroomException("Could not " + action + ": " + detail, status, e);
        } catch (IOException e) {
            throw new ClassroomException("Could not " + action + ": " + e.getMessage(), 0, e);
        }
    }
}
