package com.example.trading_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupGoalStatusResponse {
    private String groupId;
    private String groupName;
    private BigDecimal goalAmount; // 목표 금액
    private BigDecimal currentAmount; // 현재 금액 (예수금 + 수익)
    private BigDecimal progressRate; // 달성률 (%)
    private BigDecimal remainingAmount; // 남은 금액
    private boolean isGoalAchieved; // 목표 달성 여부
    private String statusMessage; // 상태 메시지
}