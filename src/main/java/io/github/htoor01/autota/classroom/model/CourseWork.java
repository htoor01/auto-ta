package io.github.htoor01.autota.classroom.model;

import java.time.LocalDate;

/** An assignment within a course. {@code maxPoints} and {@code dueDate} are null when not set. */
public record CourseWork(
        String id,
        String courseId,
        String title,
        Double maxPoints,
        LocalDate dueDate,
        String state) {
}
