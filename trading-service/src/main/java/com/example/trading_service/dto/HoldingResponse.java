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
    private Integer quantity;
    private Float avgCost;
    private Float currentPrice;
    private Float profit;
    private Float evaluatedPrice;
    private Float profitRate;
}


