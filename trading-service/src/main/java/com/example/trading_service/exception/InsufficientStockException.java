package com.example.trading_service.exception;

public class InsufficientStockException extends TradingException {
    private final int requiredQuantity;
    private final int currentQuantity;
    private final String stockName;
    
    public InsufficientStockException(String stockName, int requiredQuantity, int currentQuantity) {
        super(String.format("보유 수량 부족: %s 요청 %d주, 보유 %d주", stockName, requiredQuantity, currentQuantity));
        this.stockName = stockName;
        this.requiredQuantity = requiredQuantity;
        this.currentQuantity = currentQuantity;
    }
    
    public String getStockName() {
        return stockName;
    }
    
    public int getRequiredQuantity() {
        return requiredQuantity;
    }
    
    public int getCurrentQuantity() {
        return currentQuantity;
    }
}



