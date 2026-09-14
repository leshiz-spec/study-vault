package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class NoteNotFoundException extends ApplicationException {
  public NoteNotFoundException(Long id) {
    super(ErrorCode.NOTE_NOT_FOUND, HttpStatus.NOT_FOUND, "Note not found: " + id);
  }
}
