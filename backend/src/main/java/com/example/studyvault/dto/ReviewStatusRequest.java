package com.example.studyvault.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewStatusRequest(@NotBlank(message = "reviewStatus must not be blank") String reviewStatus) { }
