package io.github.htoor01.autota.classroom.model;

/** The downloaded bytes of an attachment. */
public record AttachmentContent(String fileName, byte[] content) {
}
