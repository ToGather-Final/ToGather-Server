package com.example.module_common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentEvent implements Serializable {
    private String eventType;
    private Long paymentId;
    private Long userId;
    private Long tradingId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private LocalDateTime timestamp;

    public PaymentEvent() {}

    public PaymentEvent(String eventType, Long paymentId, Long userId, Long tradingId, BigDecimal amount, String paymentMethod, String status) {
        this.eventType = eventType;
        this.paymentId = paymentId;
        this.userId = userId;
        this.tradingId = tradingId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTradingId() {
        return tradingId;
    }

    public void setTradingId(Long tradingId) {
        this.tradingId = tradingId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PaymentEvent{" +
                "eventType='" + eventType + '\'' +
                ", paymentId=" + paymentId +
                ", userId=" + userId +
                ", tradingId=" + tradingId +
                ", amount=" + amount +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
