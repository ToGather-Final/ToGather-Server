package com.example.trading_service.exception;

public class StockNotFoundException extends BusinessException {
    public StockNotFoundException(String stockCode) {
        super("존재하지 않는 주식입니다: " + stockCode, "STOCK_NOT_FOUND");
    }
    
    public StockNotFoundException() {
        super("존재하지 않는 주식입니다", "STOCK_NOT_FOUND");
    }
}



