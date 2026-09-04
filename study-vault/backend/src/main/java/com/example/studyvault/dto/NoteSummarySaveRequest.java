package com.example.studyvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteSummarySaveRequest(
        @NotBlank(message = "summary must not be blank")
        @Size(max = 1_000_000, message = "summary must be at most 1000000 characters")
        String summary) { }
