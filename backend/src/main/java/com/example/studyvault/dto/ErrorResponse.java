package com.example.studyvault.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/** Stable, machine-readable error details. */
public record ErrorResponse(
    String code,
    String message,
    Map<String, String> details,
    String path,
    OffsetDateTime timestamp) {
  public ErrorResponse(String code, String message, Map<String, String> details, String path) {
    this(code, message, details, path, OffsetDateTime.now());
  }
}
