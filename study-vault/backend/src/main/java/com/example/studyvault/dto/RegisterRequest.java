package com.example.studyvault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username must not be blank") @Size(min = 3, max = 50, message = "username must be 3-50 characters") String username,
        @NotBlank(message = "email must not be blank") @Email(message = "email must be valid") String email,
        @NotBlank(message = "password must not be blank") @Size(min = 8, max = 72, message = "password must be 8-72 characters") String password) { }
