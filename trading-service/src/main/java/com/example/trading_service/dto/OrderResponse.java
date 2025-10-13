package com.example.trading_service.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private UUID orderId;
    private UUID stockId;
    private String stockCode;
    private String stockName;
    private String orderType; // BUY, SELL
    private Integer quantity;
    private Float price;
    private String status; // PENDING, FILLED, CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
