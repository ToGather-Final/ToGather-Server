package com.example.trading_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookItem implements Serializable {
    private Float price; // 호가
    private Long quantity; // 잔량
    private String type; // "ask" 또는 "bid"
}
