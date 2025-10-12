package com.example.pay_service.exception;

public class UnauthorizedException extends PayServiceException {
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
