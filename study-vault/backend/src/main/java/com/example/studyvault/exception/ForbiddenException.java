package com.example.studyvault.exception;
import org.springframework.http.HttpStatus;
public class ForbiddenException extends ApplicationException {
    public ForbiddenException() { super(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "You do not have permission to perform this action"); }
}
