package com.example.studyvault.dto;

import com.example.studyvault.entity.StudyTask;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public record StudyTaskResponse(
    Long id,
    String title,
    LocalDate dueDate,
    String status,
    Long noteId,
    String noteTitle,
    List<Long> noteIds,
    List<String> noteTitles,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {
  public static StudyTaskResponse from(StudyTask task) {
    var linkedNotes = new ArrayList<>(task.getNotes());
    var note = linkedNotes.isEmpty() ? null : linkedNotes.get(0);
    return new StudyTaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDueDate(),
        task.getStatus(),
        note == null ? null : note.getId(),
        note == null ? null : note.getTitle(),
        linkedNotes.stream().map(com.example.studyvault.entity.Note::getId).toList(),
        linkedNotes.stream().map(com.example.studyvault.entity.Note::getTitle).toList(),
        task.getCreatedAt(),
        task.getUpdatedAt());
  }

  public StudyTaskResponse(
      Long id,
      String title,
      LocalDate dueDate,
      String status,
      Long noteId,
      String noteTitle,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {
    this(
        id,
        title,
        dueDate,
        status,
        noteId,
        noteTitle,
        noteId == null ? List.of() : List.of(noteId),
        noteTitle == null ? List.of() : List.of(noteTitle),
        createdAt,
        updatedAt);
  }
}
