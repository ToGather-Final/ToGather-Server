package com.example.trading_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SimpleChartData {
    private String date;           // 날짜 (YYYYMMDD)
    private Float open;            // 시가
    private Float high;            // 고가
    private Float low;             // 저가
    private Float close;           // 종가
    private Long volume;           // 거래량
}


