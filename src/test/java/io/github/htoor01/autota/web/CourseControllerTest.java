package io.github.htoor01.autota.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.htoor01.autota.classroom.ClassroomClient;
import io.github.htoor01.autota.classroom.ClassroomException;
import io.github.htoor01.autota.classroom.ResourceNotFoundException;
import io.github.htoor01.autota.classroom.model.Attachment;
import io.github.htoor01.autota.classroom.model.Course;
import io.github.htoor01.autota.classroom.model.CourseWork;
import io.github.htoor01.autota.classroom.model.Submission;

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClassroomClient classroomClient;

    @Test
    void listsCourses() throws Exception {
        when(classroomClient.listCourses())
                .thenReturn(List.of(new Course("c-1", "Intro to Java", "2026 Batch", "ACTIVE")));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("c-1"))
                .andExpect(jsonPath("$[0].name").value("Intro to Java"));
    }

    @Test
    void listsCourseWorkWithIsoDueDate() throws Exception {
        when(classroomClient.listCourseWork("c-1")).thenReturn(List.of(
                new CourseWork("cw-1", "c-1", "Calculator", 100.0, LocalDate.of(2026, 10, 20), "PUBLISHED")));

        mockMvc.perform(get("/api/courses/c-1/coursework"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Calculator"))
                .andExpect(jsonPath("$[0].dueDate").value("2026-10-20"));
    }

    @Test
    void listsSubmissions() throws Exception {
        when(classroomClient.listSubmissions("c-1", "cw-1")).thenReturn(List.of(
                new Submission("s-1", "c-1", "cw-1", "u-1", "Student A", "TURNED_IN", false, null, null,
                        List.of(new Attachment("f-1", "Calculator.java")))));

        mockMvc.perform(get("/api/courses/c-1/coursework/cw-1/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Student A"))
                .andExpect(jsonPath("$[0].attachments[0].title").value("Calculator.java"));
    }

    @Test
    void notFoundBecomesProblem404() throws Exception {
        when(classroomClient.listCourseWork("missing"))
                .thenThrow(new ResourceNotFoundException("Course missing not found"));

        mockMvc.perform(get("/api/courses/missing/coursework"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Course missing not found"));
    }

    @Test
    void classroomPermissionErrorBecomes403() throws Exception {
        when(classroomClient.listCourses()).thenThrow(new ClassroomException("denied", 403, null));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.upstreamStatus").value(403));
    }

    @Test
    void otherClassroomErrorsBecome502() throws Exception {
        when(classroomClient.listCourses()).thenThrow(new ClassroomException("unavailable", 503, null));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isBadGateway());
    }
}
