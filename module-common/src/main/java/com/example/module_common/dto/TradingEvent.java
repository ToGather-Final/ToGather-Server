package com.example.module_common.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradingEvent implements Serializable {
    private String eventType;
    private Long tradingId;
    private Long userId;
    private String itemName;
    private BigDecimal price;
    private String status;
    private LocalDateTime timestamp;

    public TradingEvent() {}

    public TradingEvent(String eventType, Long tradingId, Long userId, String itemName, BigDecimal price, String status) {
        this.eventType = eventType;
        this.tradingId = tradingId;
        this.userId = userId;
        this.itemName = itemName;
        this.price = price;
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

    public Long getTradingId() {
        return tradingId;
    }

    public void setTradingId(Long tradingId) {
        this.tradingId = tradingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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
        return "TradingEvent{" +
                "eventType='" + eventType + '\'' +
                ", tradingId=" + tradingId +
                ", userId=" + userId +
                ", itemName='" + itemName + '\'' +
                ", price=" + price +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
