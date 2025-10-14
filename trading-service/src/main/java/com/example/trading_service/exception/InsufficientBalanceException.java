package com.example.trading_service.exception;

import java.util.Map;

public class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(float required, float available) {
        super("잔고가 부족합니다", "INSUFFICIENT_BALANCE", 
              Map.of("required", required, "available", available));
    }
}

