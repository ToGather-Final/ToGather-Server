package com.example.vote_service.dto.payload;

/**
 * 매매 실패 페이로드 DTO
 */
public record TradeFailedPayloadDTO(
        String side,              // "BUY" | "SELL"
        String stockName,
        String reason             // 실패 사유
) {
}
