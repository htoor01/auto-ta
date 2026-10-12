package io.github.htoor01.autota.classroom.model;

/** A file attached to a submission. Classroom stores attachments as Drive files. */
public record Attachment(String driveFileId, String title) {
}
