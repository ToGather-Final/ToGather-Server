package com.example.pay_service.exception;

public class PayServiceException extends RuntimeException {
    private final String code;

    public PayServiceException(String code, String message) {
        super(message);
        this.code = code;
    }

    public PayServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
