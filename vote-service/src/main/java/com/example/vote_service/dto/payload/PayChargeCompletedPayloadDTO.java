package com.example.vote_service.dto.payload;

/**
 * 페이 충전 완료 페이로드 DTO
 */
public record PayChargeCompletedPayloadDTO(
        Integer amount,           // 금액
        Integer accountBalance    // 이후 모임계좌잔액
) {
}
