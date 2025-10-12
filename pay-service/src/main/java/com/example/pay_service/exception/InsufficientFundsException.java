package com.example.pay_service.exception;

public class InsufficientFundsException extends PayServiceException {
    public InsufficientFundsException(String message) {
        super("INSUFFICIENT_FUNDS", message);
    }
}
