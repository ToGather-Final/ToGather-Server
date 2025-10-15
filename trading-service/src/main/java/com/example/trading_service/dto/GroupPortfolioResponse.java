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
public class GroupPortfolioResponse {
    private String groupId;
    private String groupName;
    private Integer memberCount;
    private BigDecimal totalValue; // 총 자산 가치
    private BigDecimal totalInvestment; // 총 투자 금액
    private BigDecimal totalProfit; // 총 수익
    private BigDecimal profitRate; // 수익률
    private List<GroupHoldingInfo> holdings; // 그룹 보유 종목
    private BigDecimal goalAmount; // 목표 금액
    private BigDecimal goalProgress; // 목표 달성률
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class GroupHoldingInfo {
    private String stockCode;
    private String stockName;
    private Integer totalQuantity;
    private BigDecimal avgCost;
    private BigDecimal currentPrice;
    private BigDecimal totalValue;
    private BigDecimal profit;
    private BigDecimal profitRate;
}
