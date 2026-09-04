package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class AiProviderException extends ApplicationException {
    public AiProviderException() {
        super(ErrorCode.AI_PROVIDER_ERROR, HttpStatus.BAD_GATEWAY,
                "The AI service returned an error");
    }

    public AiProviderException(String message) {
        super(ErrorCode.AI_PROVIDER_ERROR, HttpStatus.BAD_GATEWAY, message);
    }
}
