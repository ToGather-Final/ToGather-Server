package com.example.vote_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal Deposit Request DTO
 * - trading-service의 internal deposit API 호출용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InternalDepositRequest {
    
    @NotNull(message = "사용자 ID는 필수입니다.")
    private UUID userId;
    
    @NotNull(message = "충전 금액은 필수입니다.")
    @Min(value = 1, message = "충전 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;
    
    @NotNull(message = "그룹 ID는 필수입니다.")
    private UUID groupId;
    
    private String description; // "투표 가결에 따른 예수금 충전" 등
}
