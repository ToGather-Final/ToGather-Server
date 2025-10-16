package com.example.vote_service.dto.payload;

/**
 * 예수금 충전 완료 페이로드 DTO
 */
public record CashDepositCompletedPayloadDTO(
        String depositorName,     // 충전자
        Integer amount,           // 금액
        Integer accountBalance    // 이후 모임계좌잔액
) {
}
