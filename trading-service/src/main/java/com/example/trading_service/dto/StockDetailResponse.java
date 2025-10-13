package com.example.trading_service.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class StockDetailResponse {
    private UUID stockId;
    private String stockCode;
    private String stockName;
    private String market; // KOSPI, KOSDAQ 등
    private Float currentPrice; // 현재가
    private Float changeAmount; // 전일 대비 변동금액
    private Float changeRate; // 전일 대비 변동률
    private String changeDirection; // "up", "down", "unchanged"
    private Long volume; // 거래량
    private Float highPrice; // 고가
    private Float lowPrice; // 저가
    private Float openPrice; // 시가
    private Float prevClosePrice; // 전일종가
    private Long marketCap; // 시가총액
    private List<ChartData> chartData; // 차트 데이터
    private Float resistanceLine; // 저항선 (상한가)
    private Float supportLine; // 지지선 (하한가)
}

