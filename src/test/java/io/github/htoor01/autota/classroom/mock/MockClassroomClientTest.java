package io.github.htoor01.autota.classroom.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import io.github.htoor01.autota.classroom.ResourceNotFoundException;
import io.github.htoor01.autota.classroom.model.AttachmentContent;
import io.github.htoor01.autota.classroom.model.Submission;

class MockClassroomClientTest {

    private static final String COURSE = "course-java-2026";
    private static final String CALCULATOR = "cw-calculator";

    private MockClassroomClient client;

    @BeforeEach
    void setUp() {
        client = new MockClassroomClient(JsonMapper.builder().build());
    }

    @Test
    void listsSampleCourses() {
        assertThat(client.listCourses())
                .singleElement()
                .satisfies(course -> assertThat(course.name()).isEqualTo("Intro to Java"));
    }

    @Test
    void listsCourseWorkWithParsedDueDates() {
        assertThat(client.listCourseWork(COURSE))
                .hasSize(2)
                .first()
                .satisfies(work -> {
                    assertThat(work.id()).isEqualTo(CALCULATOR);
                    assertThat(work.dueDate()).isEqualTo(LocalDate.of(2026, 10, 20));
                });
    }

    @Test
    void unknownCourseIsNotFound() {
        assertThatThrownBy(() -> client.listCourseWork("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listsSubmissionsForCourseWork() {
        assertThat(client.listSubmissions(COURSE, CALCULATOR))
                .hasSize(5)
                .extracting(Submission::studentName)
                .contains("Student A", "Student E");
        assertThat(client.listSubmissions(COURSE, "cw-strings")).isEmpty();
    }

    @Test
    void unknownCourseWorkIsNotFound() {
        assertThatThrownBy(() -> client.listSubmissions(COURSE, "nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void downloadsAttachmentContent() {
        AttachmentContent file = client.downloadAttachment("file-001");

        assertThat(file.fileName()).isEqualTo("Calculator.java");
        assertThat(new String(file.content(), StandardCharsets.UTF_8)).contains("public class Calculator");
    }

    @Test
    void unknownAttachmentIsNotFound() {
        assertThatThrownBy(() -> client.downloadAttachment("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
