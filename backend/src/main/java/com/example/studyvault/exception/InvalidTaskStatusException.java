package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class InvalidTaskStatusException extends ApplicationException {
    public InvalidTaskStatusException(String status) { super(ErrorCode.INVALID_TASK_STATUS, HttpStatus.BAD_REQUEST, "Invalid task status: " + status); }
}
