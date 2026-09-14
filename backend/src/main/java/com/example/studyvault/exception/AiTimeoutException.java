package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class AiTimeoutException extends ApplicationException {
  public AiTimeoutException() {
    super(ErrorCode.AI_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT, "The AI service timed out");
  }
}
