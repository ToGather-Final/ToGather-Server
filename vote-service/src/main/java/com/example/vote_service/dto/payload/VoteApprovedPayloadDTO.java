package com.example.vote_service.dto.payload;

import java.util.UUID;

/**
 * 투표 가결 페이로드 DTO
 */
public record VoteApprovedPayloadDTO(
        UUID proposalId,
        String scheduledAt,
        String historyType,       // "TRADE" | "PAY"
        String side,              // "BUY" | "SELL" | "PAY"
        String stockName,
        Integer shares,
        Integer unitPrice,
        String currency           // "KRW" | "USD"
) {
}
