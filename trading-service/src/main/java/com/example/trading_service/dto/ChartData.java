package com.example.trading_service.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class ChartData {
    private String time;           // 날짜 (YYYY-MM-DD)
    private Float open;            // 시가
    private Float high;            // 고가
    private Float low;             // 저가
    private Float close;           // 종가
    private Float ma_5;            // 5일 이동평균
    private Float ma_20;           // 20일 이동평균
    private Float ma_60;           // 60일 이동평균
    private Float ma_120;          // 120일 이동평균
    private Long trading_volume;   // 거래량
}