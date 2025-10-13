package com.example.trading_service.exception;

public class TradingException extends RuntimeException {
    public TradingException(String message) {
        super(message);
    }
    
    public TradingException(String message, Throwable cause) {
        super(message, cause);
    }
}



