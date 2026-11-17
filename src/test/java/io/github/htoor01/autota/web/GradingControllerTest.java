package io.github.htoor01.autota.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.htoor01.autota.classroom.ResourceNotFoundException;
import io.github.htoor01.autota.grader.GradeStatus;
import io.github.htoor01.autota.grader.GradingQueue;
import io.github.htoor01.autota.grader.GradingQueueFullException;
import io.github.htoor01.autota.grader.GradingResult;
import io.github.htoor01.autota.grader.GradingRun;
import io.github.htoor01.autota.grader.GradingRunRepository;
import io.github.htoor01.autota.grader.GradingRunStatus;

@WebMvcTest(GradingController.class)
class GradingControllerTest {

    private static final String GRADE = "/api/courses/c-1/coursework/cw-1/grade";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GradingQueue gradingQueue;

    @MockitoBean
    private GradingRunRepository repository;

    @Test
    void gradingIsAcceptedAndPointsToTheRun() throws Exception {
        when(gradingQueue.submit("c-1", "cw-1")).thenReturn(run("run-1", GradingRunStatus.QUEUED, List.of()));

        mockMvc.perform(post(GRADE))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", "/api/grading-runs/run-1"))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-20T12:00:00Z"));
    }

    @Test
    void unknownAssignmentIsRejectedBeforeQueueing() throws Exception {
        when(gradingQueue.submit("c-1", "cw-1")).thenThrow(new ResourceNotFoundException("No grader tests found"));

        mockMvc.perform(post(GRADE)).andExpect(status().isNotFound());
    }

    @Test
    void fullQueueReturns503WithRetryAfter() throws Exception {
        when(gradingQueue.submit("c-1", "cw-1")).thenThrow(new GradingQueueFullException("busy"));

        mockMvc.perform(post(GRADE))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "30"));
    }

    @Test
    void returnsRunWithResults() throws Exception {
        GradingResult result = new GradingResult("s-1", "student-1", "Student A", GradeStatus.GRADED, 7, 7, 100.0, 100.0,
                List.of(), 800, null);
        when(repository.find("run-1")).thenReturn(Optional.of(run("run-1", GradingRunStatus.COMPLETED, List.of(result))));

        mockMvc.perform(get("/api/grading-runs/run-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.results[0].suggestedGrade").value(100.0));
    }

    @Test
    void unknownRunIs404() throws Exception {
        when(repository.find("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/grading-runs/nope")).andExpect(status().isNotFound());
    }

    private static GradingRun run(String id, GradingRunStatus status, List<GradingResult> results) {
        return new GradingRun(id, "c-1", "cw-1", status, Instant.parse("2026-10-20T12:00:00Z"), null, null, null, results);
    }
}
