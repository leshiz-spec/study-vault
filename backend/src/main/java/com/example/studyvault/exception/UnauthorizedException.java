package com.example.studyvault.exception;
import org.springframework.http.HttpStatus;
public class UnauthorizedException extends ApplicationException {
    public UnauthorizedException() { super(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "Authentication is required"); }
}
