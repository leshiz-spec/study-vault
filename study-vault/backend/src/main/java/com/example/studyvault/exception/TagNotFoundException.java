package com.example.studyvault.exception;
import org.springframework.http.HttpStatus;
public class TagNotFoundException extends ApplicationException {
    public TagNotFoundException(Long id) { super(ErrorCode.TAG_NOT_FOUND, HttpStatus.NOT_FOUND, "Tag not found: " + id); }
}
