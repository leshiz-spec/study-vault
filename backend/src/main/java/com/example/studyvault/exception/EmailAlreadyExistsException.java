package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApplicationException {
  public EmailAlreadyExistsException() {
    super(ErrorCode.EMAIL_ALREADY_EXISTS, HttpStatus.CONFLICT, "Email is already registered");
  }
}
