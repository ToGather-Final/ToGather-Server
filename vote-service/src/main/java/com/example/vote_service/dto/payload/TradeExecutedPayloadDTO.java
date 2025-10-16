package com.example.vote_service.dto.payload;

/**
 * 매매 완료 페이로드 DTO
 */
public record TradeExecutedPayloadDTO(
        String side,              // "BUY" | "SELL"
        String stockName,
        Integer shares,
        Integer unitPrice,
        Integer accountBalance    // 체결 후 모임계좌잔액
) {
}
