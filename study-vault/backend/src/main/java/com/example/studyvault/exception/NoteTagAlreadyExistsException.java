package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class NoteTagAlreadyExistsException extends ApplicationException {
    public NoteTagAlreadyExistsException() { super(ErrorCode.NOTE_TAG_ALREADY_EXISTS, HttpStatus.CONFLICT, "Tag is already assigned to this note"); }
}
