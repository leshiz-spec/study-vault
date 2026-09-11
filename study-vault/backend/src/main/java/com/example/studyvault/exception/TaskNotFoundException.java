package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class TaskNotFoundException extends ApplicationException {
    public TaskNotFoundException(Long id) { super(ErrorCode.TASK_NOT_FOUND, HttpStatus.NOT_FOUND, "Study task not found: " + id); }
}
