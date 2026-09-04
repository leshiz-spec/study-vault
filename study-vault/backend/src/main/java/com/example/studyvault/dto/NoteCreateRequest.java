package com.example.studyvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Fields accepted when creating a note. Ownership is always taken from authentication. */
public record NoteCreateRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,
        @NotBlank(message = "content must not be blank")
        @Size(max = 1_000_000, message = "content must be at most 1000000 characters")
        String content) { }
