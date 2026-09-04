package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class FileTooLargeException extends ApplicationException {
    public FileTooLargeException(String message) {
        super(ErrorCode.FILE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE, message);
    }
}
