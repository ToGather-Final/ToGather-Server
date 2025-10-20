package com.example.pay_service.dto;

import java.time.LocalDateTime;

public record UnifiedHistoryItem(
        String id,
        String type,
        long amount,
        long balanceAfter,
        String description,
        LocalDateTime createdAt
) {
}
