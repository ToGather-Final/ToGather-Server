package com.example.pay_service.exception;

public class SessionNotFoundException extends PayServiceException {
    public SessionNotFoundException(String message) {
        super("SESSION_NOT_FOUND", message);
    }
}
