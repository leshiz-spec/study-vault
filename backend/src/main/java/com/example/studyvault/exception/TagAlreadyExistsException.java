package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class TagAlreadyExistsException extends ApplicationException {
  public TagAlreadyExistsException() {
    super(ErrorCode.TAG_ALREADY_EXISTS, HttpStatus.CONFLICT, "A tag with this name already exists");
  }
}
