package com.example.studyvault.dto;

import com.example.studyvault.entity.NoteRevision;
import java.time.OffsetDateTime;

/** Safe DTO for a historical note snapshot. */
public record NoteRevisionResponse(
    Long id, Long noteId, String title, String content, OffsetDateTime createdAt) {
  public static NoteRevisionResponse from(NoteRevision revision) {
    return new NoteRevisionResponse(
        revision.getId(),
        revision.getNote().getId(),
        revision.getTitle(),
        revision.getContent(),
        revision.getCreatedAt());
  }
}
