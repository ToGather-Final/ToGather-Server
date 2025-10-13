package com.example.pay_service.exception;

public class TokenInvalidException extends PayServiceException {
    public TokenInvalidException(String message) {
        super("TOKEN_INVALID", message);
    }
}
