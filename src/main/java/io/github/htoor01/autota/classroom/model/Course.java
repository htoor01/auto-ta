package io.github.htoor01.autota.classroom.model;

/** A Classroom course. {@code state} is Classroom's course state, e.g. {@code ACTIVE}. */
public record Course(String id, String name, String section, String state) {
}
