package io.github.htoor01.autota.web;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.htoor01.autota.grader.GradingResult;
import io.github.htoor01.autota.grader.GradingService;

/**
 * Runs the grader. Results are suggestions for the instructor to approve; nothing is sent to Classroom.
 *
 * <p>The request blocks until every submission is graded (a few seconds each).
 */
@RestController
@RequestMapping("/api/courses/{courseId}/coursework/{courseWorkId}")
class GradingController {

    private final GradingService gradingService;

    GradingController(GradingService gradingService) {
        this.gradingService = gradingService;
    }

    @PostMapping("/grade")
    List<GradingResult> grade(@PathVariable String courseId, @PathVariable String courseWorkId) {
        return gradingService.gradeAssignment(courseId, courseWorkId);
    }
}
