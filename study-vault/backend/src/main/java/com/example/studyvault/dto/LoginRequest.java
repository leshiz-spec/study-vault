package com.example.studyvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

    public record LoginRequest(@NotBlank(message = "username or email must not be blank") @Size(max = 255, message = "username or email is too long") String usernameOrEmail,
                           @NotBlank(message = "password must not be blank") @Size(max = 72, message = "password must be at most 72 characters") String password) { }
