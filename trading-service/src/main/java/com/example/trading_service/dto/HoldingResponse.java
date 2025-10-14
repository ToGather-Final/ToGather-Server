package com.example.trading_service.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class HoldingResponse {
    private UUID holdingId;
    private UUID stockId;
    private String stockCode;
    private String stockName;
    private String stockImage; // 주식 로고/이미지
    private Integer quantity;
    private Float avgCost;
    private Float currentPrice;
    private Float changeAmount; // 전일 대비 변동금액
    private Float changeRate; // 전일 대비 변동률
    private Float profit; // 평가손익
    private Float evaluatedPrice; // 평가금액
    private Float profitRate; // 수익률
    private String changeDirection; // "up", "down", "unchanged"
}


