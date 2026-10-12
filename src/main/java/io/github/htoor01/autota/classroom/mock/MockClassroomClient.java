package io.github.htoor01.autota.classroom.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import io.github.htoor01.autota.classroom.ClassroomClient;
import io.github.htoor01.autota.classroom.ResourceNotFoundException;
import io.github.htoor01.autota.classroom.model.Attachment;
import io.github.htoor01.autota.classroom.model.AttachmentContent;
import io.github.htoor01.autota.classroom.model.Course;
import io.github.htoor01.autota.classroom.model.CourseWork;
import io.github.htoor01.autota.classroom.model.Submission;

/**
 * {@link ClassroomClient} that serves sample data from {@code src/main/resources/data/mock}, so AutoTA
 * runs without Google credentials.
 */
@Component
@Profile("mock")
class MockClassroomClient implements ClassroomClient {

    private static final String DATA_DIR = "data/mock/";

    private final List<Course> courses;
    private final List<CourseWork> courseWork;
    private final List<Submission> submissions;

    MockClassroomClient(JsonMapper jsonMapper) {
        this.courses = List.of(read(jsonMapper, "courses.json", Course[].class));
        this.courseWork = List.of(read(jsonMapper, "coursework.json", CourseWork[].class));
        this.submissions = List.of(read(jsonMapper, "submissions.json", Submission[].class));
    }

    @Override
    public List<Course> listCourses() {
        return courses;
    }

    @Override
    public List<CourseWork> listCourseWork(String courseId) {
        requireCourse(courseId);
        return courseWork.stream()
                .filter(work -> work.courseId().equals(courseId))
                .toList();
    }

    @Override
    public List<Submission> listSubmissions(String courseId, String courseWorkId) {
        requireCourseWork(courseId, courseWorkId);
        return submissions.stream()
                .filter(submission -> submission.courseWorkId().equals(courseWorkId))
                .toList();
    }

    @Override
    public AttachmentContent downloadAttachment(String driveFileId) {
        Attachment attachment = submissions.stream()
                .flatMap(submission -> submission.attachments().stream())
                .filter(candidate -> candidate.driveFileId().equals(driveFileId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("File " + driveFileId + " not found"));

        ClassPathResource file = new ClassPathResource(DATA_DIR + "files/" + driveFileId + "/" + attachment.title());
        try (InputStream in = file.getInputStream()) {
            return new AttachmentContent(attachment.title(), in.readAllBytes());
        } catch (IOException e) {
            throw new ResourceNotFoundException("File " + driveFileId + " has no mock content at " + file.getPath());
        }
    }

    private void requireCourse(String courseId) {
        if (courses.stream().noneMatch(course -> course.id().equals(courseId))) {
            throw new ResourceNotFoundException("Course " + courseId + " not found");
        }
    }

    private void requireCourseWork(String courseId, String courseWorkId) {
        requireCourse(courseId);
        boolean exists = courseWork.stream()
                .anyMatch(work -> work.courseId().equals(courseId) && work.id().equals(courseWorkId));
        if (!exists) {
            throw new ResourceNotFoundException("Coursework " + courseWorkId + " not found in course " + courseId);
        }
    }

    private static <T> T read(JsonMapper jsonMapper, String fileName, Class<T> type) {
        try (InputStream in = new ClassPathResource(DATA_DIR + fileName).getInputStream()) {
            return jsonMapper.readValue(in, type);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load mock data " + DATA_DIR + fileName, e);
        }
    }
}
