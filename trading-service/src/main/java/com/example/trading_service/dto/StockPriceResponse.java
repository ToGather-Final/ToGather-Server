package com.example.trading_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StockPriceResponse {
    
    private BigDecimal currentPrice;        // 현재가
    private BigDecimal changePrice;         // 전일 대비 변동가격
    private Float changeRate;          // 전일 대비 변동률
    private Long volume;                    // 거래량
    private BigDecimal highPrice;           // 고가
    private BigDecimal lowPrice;            // 저가
    private BigDecimal openPrice;           // 시가
    private BigDecimal prevClosePrice;      // 전일 종가
}


