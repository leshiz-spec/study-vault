package com.example.studyvault.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NoteTagRequest(@NotNull(message = "tagId must not be null") @Positive(message = "tagId must be positive") Long tagId) { }
