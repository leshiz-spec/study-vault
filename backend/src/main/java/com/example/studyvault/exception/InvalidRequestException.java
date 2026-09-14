package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends ApplicationException {
  public InvalidRequestException(String message) {
    super(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST, message);
  }
}
