package com.example.studyvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagUpdateRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 80, message = "name must be at most 80 characters") String name,
        @Size(max = 20, message = "color must be at most 20 characters") String color) { }
