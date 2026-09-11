package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class AiNotConfiguredException extends ApplicationException {
    public AiNotConfiguredException() {
        super(ErrorCode.AI_NOT_CONFIGURED, HttpStatus.SERVICE_UNAVAILABLE,
                "AI summaries are not configured");
    }
}
