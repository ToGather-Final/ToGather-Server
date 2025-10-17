package com.example.trading_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfoResponse {
    private String stockId;
    private String stockCode;
    private String stockName;
    private String market;
    private BigDecimal currentPrice;
    private BigDecimal changeAmount;
    private Float changeRate;
    private String changeDirection; // "up", "down", "unchanged"
    private Long volume;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal openPrice;
    private BigDecimal prevClosePrice;
    private Long marketCap;
    private Object chartData; // ChartData 또는 SimpleChartData
    private BigDecimal resistanceLine;
    private BigDecimal supportLine;
}


