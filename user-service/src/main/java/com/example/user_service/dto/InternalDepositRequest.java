package com.example.user_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal Deposit Request DTO
 * - trading-service와 통신용 예수금 충전 요청
 */
public record InternalDepositRequest(
        UUID userId,
        BigDecimal amount,
        UUID groupId,
        String description
) {
}

