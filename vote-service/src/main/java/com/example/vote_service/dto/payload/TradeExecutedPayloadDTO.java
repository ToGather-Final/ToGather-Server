package com.example.vote_service.dto.payload;

/**
 * 매매 완료 페이로드 DTO
 */
public record TradeExecutedPayloadDTO(
        String side,              // "BUY" | "SELL"
        String stockName,
        Float shares,             // 소수점 거래 지원을 위해 Float으로 변경
        Integer unitPrice,
        Integer accountBalance    // 체결 후 모임계좌잔액
) {
}
