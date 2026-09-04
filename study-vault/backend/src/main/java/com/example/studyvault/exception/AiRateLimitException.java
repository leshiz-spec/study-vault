package com.example.studyvault.exception;

import org.springframework.http.HttpStatus;

public class AiRateLimitException extends ApplicationException {
    public AiRateLimitException() {
        super(ErrorCode.AI_RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS,
                "The AI service rate limit was reached");
    }
}
