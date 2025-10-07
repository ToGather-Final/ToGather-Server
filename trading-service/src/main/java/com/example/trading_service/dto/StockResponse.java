package com.example.trading_service.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class StockResponse {
    private UUID stockId;
    private String stockCode;
    private String stockName;
    private String stockImage;
    private String country;
    private Float currentPrice;
    private Float changeAmount; // 전일 대비 변동금액
    private Float changeRate; // 전일 대비 변동률
    private Boolean enabled;
}


