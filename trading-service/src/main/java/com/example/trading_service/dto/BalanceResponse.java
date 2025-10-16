package com.example.trading_service.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class BalanceResponse {
    private UUID balanceId;
    private UUID investmentAccountId;
    private Long balance;
    private Float totalInvested; // 총 투자금액
    private Float totalValue; // 총 평가금액
    private Float totalProfit; // 총 평가손익
    private Float totalProfitRate; // 총 수익률
}