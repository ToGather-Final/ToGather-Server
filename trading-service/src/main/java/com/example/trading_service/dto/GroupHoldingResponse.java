package com.example.trading_service.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class GroupHoldingResponse {
    private UUID groupHoldingId;
    private UUID groupId;
    private UUID stockId;
    private String stockCode;
    private String stockName;
    private String stockImage; // 주식 로고/이미지
    private Integer totalQuantity; // 그룹 전체 보유 수량
    private Float avgCost; // 평균 매입 단가
    private Float currentPrice; // 현재가
    private Float changeAmount; // 전일 대비 변동금액
    private Float changeRate; // 전일 대비 변동률
    private Float profit; // 평가손익
    private Float evaluatedPrice; // 평가금액
    private Float profitRate; // 수익률
    private String changeDirection; // "up", "down", "unchanged"
    private Integer memberCount; // 그룹 멤버 수
    private Float avgQuantityPerMember; // 멤버당 평균 보유 수량
}
