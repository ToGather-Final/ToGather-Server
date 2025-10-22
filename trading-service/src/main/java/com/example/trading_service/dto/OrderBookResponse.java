package com.example.trading_service.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookResponse {
    private String stockCode;
    private String stockName;
    private Float currentPrice; // 현재가
    private Float changeAmount; // 전일 대비 변동금액
    private Float changeRate; // 전일 대비 변동률
    private String changeDirection; // "up", "down", "unchanged"
    private List<OrderBookItem> askPrices; // 매도 호가 (빨간색 - 위쪽)
    private List<OrderBookItem> bidPrices; // 매수 호가 (파란색 - 아래쪽)
}

