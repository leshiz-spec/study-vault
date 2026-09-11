package com.example.studyvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record StudyTaskUpdateRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must be at most 255 characters") String title,
        LocalDate dueDate,
        String status,
        Long noteId,
        List<Long> noteIds) {
    public StudyTaskUpdateRequest(String title, LocalDate dueDate, String status, Long noteId) {
        this(title, dueDate, status, noteId, noteId == null ? List.of() : List.of(noteId));
    }
}
