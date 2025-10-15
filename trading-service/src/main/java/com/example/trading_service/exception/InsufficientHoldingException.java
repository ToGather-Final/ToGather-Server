package com.example.trading_service.exception;

import java.util.Map;

public class InsufficientHoldingException extends BusinessException {
    public InsufficientHoldingException(int required, int available) {
        super("보유 수량이 부족합니다", "INSUFFICIENT_HOLDING", 
              Map.of("required", required, "available", available));
    }
}

