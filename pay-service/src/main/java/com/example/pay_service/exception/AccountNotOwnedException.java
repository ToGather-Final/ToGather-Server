package com.example.pay_service.exception;

public class AccountNotOwnedException extends PayServiceException {
    public AccountNotOwnedException(String message) {
        super("ACCOUNT_NOT_OWNED", message);
    }
}
