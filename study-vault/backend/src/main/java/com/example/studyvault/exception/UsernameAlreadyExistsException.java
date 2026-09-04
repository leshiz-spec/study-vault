package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends ApplicationException {
    public UsernameAlreadyExistsException() {
        super(ErrorCode.USERNAME_ALREADY_EXISTS, HttpStatus.CONFLICT, "Username is already registered");
    }
}
