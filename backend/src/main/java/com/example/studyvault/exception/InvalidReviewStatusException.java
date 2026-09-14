package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class InvalidReviewStatusException extends ApplicationException {
  public InvalidReviewStatusException(String status) {
    super(
        ErrorCode.INVALID_REVIEW_STATUS,
        HttpStatus.BAD_REQUEST,
        "Invalid review status: " + status);
  }
}
