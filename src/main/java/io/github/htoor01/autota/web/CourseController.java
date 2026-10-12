package io.github.htoor01.autota.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.htoor01.autota.classroom.ClassroomClient;
import io.github.htoor01.autota.classroom.model.Course;
import io.github.htoor01.autota.classroom.model.CourseWork;
import io.github.htoor01.autota.classroom.model.Submission;

/**
 * Read-only access to courses, assignments and submissions.
 *
 * <p>There is deliberately no endpoint for pushing grades: grades must go through instructor approval first.
 */
@RestController
@RequestMapping("/api/courses")
class CourseController {

    private final ClassroomClient classroomClient;

    CourseController(ClassroomClient classroomClient) {
        this.classroomClient = classroomClient;
    }

    @GetMapping
    List<Course> listCourses() {
        return classroomClient.listCourses();
    }

    @GetMapping("/{courseId}/coursework")
    List<CourseWork> listCourseWork(@PathVariable String courseId) {
        return classroomClient.listCourseWork(courseId);
    }

    @GetMapping("/{courseId}/coursework/{courseWorkId}/submissions")
    List<Submission> listSubmissions(@PathVariable String courseId, @PathVariable String courseWorkId) {
        return classroomClient.listSubmissions(courseId, courseWorkId);
    }
}
