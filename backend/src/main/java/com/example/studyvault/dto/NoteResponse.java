package com.example.studyvault.dto;

import com.example.studyvault.entity.Note;
import java.time.OffsetDateTime;
import java.util.List;

/** Safe API projection of a note; the owning user entity is never serialized. */
public record NoteResponse(
    Long id,
    String title,
    String content,
    String summary,
    String status,
    boolean favorite,
    String reviewStatus,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<TagResponse> tags) {
  public NoteResponse(
      Long id,
      String title,
      String content,
      String summary,
      String status,
      boolean favorite,
      String reviewStatus,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {
    this(
        id,
        title,
        content,
        summary,
        status,
        favorite,
        reviewStatus,
        createdAt,
        updatedAt,
        List.of());
  }

  public static NoteResponse from(Note note) {
    return new NoteResponse(
        note.getId(),
        note.getTitle(),
        note.getContent(),
        note.getSummary(),
        note.getStatus(),
        note.isFavorite(),
        note.getReviewStatus(),
        note.getCreatedAt(),
        note.getUpdatedAt(),
        List.of());
  }
}
