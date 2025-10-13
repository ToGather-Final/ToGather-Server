package com.example.trading_service.exception;

public class InsufficientBalanceException extends TradingException {
    private final long requiredAmount;
    private final long currentBalance;
    
    public InsufficientBalanceException(long requiredAmount, long currentBalance) {
        super(String.format("예수금 부족: 필요 %d원, 보유 %d원", requiredAmount, currentBalance));
        this.requiredAmount = requiredAmount;
        this.currentBalance = currentBalance;
    }
    
    public long getRequiredAmount() {
        return requiredAmount;
    }
    
    public long getCurrentBalance() {
        return currentBalance;
    }
}



