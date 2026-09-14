package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

/** Raised when an uploaded Markdown file cannot be accepted as a note. */
public class InvalidMarkdownFileException extends ApplicationException {
  public InvalidMarkdownFileException(String message) {
    super(ErrorCode.INVALID_MARKDOWN_FILE, HttpStatus.BAD_REQUEST, message);
  }
}
