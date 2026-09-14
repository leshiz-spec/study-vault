package com.example.studyvault.dto;

import com.example.studyvault.entity.User;
import java.time.OffsetDateTime;

/** Public user projection; passwordHash is intentionally never exposed. */
public record UserResponse(
    Long id, String username, String email, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
