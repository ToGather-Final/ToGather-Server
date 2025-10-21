package com.example.trading_service.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class OrderBookItem {
    private Float price; // 호가
    private Long quantity; // 잔량
    private String type; // "ask" 또는 "bid"
}
