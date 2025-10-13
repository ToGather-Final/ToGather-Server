package com.example.trading_service.exception;

public class InvalidOrderException extends TradingException {
    public InvalidOrderException(String message) {
        super(message);
    }
    
    public InvalidOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}



