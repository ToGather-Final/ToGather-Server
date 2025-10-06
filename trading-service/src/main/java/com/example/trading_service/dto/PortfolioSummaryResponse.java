package com.example.trading_service.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
public class PortfolioSummaryResponse {
    private float totalInvested; // 총 투자금액
    private float totalValue; // 총 평가금액
    private float totalProfit; // 총 평가손익
    private float totalProfitRate; // 총 수익률
    private int totalHoldings; // 보유 종목 수
    private List<HoldingResponse> topHoldings; // 상위 보유 종목 (5개)
}
