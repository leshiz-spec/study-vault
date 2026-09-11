package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

/** Base exception carrying an API-stable error code and HTTP status. */
public class ApplicationException extends RuntimeException {
    private final ErrorCode code;
    private final HttpStatus status;

    public ApplicationException(ErrorCode code, HttpStatus status, String message) {
        super(message); this.code = code; this.status = status;
    }
    public ErrorCode getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
