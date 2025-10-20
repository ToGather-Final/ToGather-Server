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
        Float shares,             // 소수점 거래 지원을 위해 Float으로 변경
        Integer unitPrice,
        String currency           // "KRW" | "USD"
) {
}
