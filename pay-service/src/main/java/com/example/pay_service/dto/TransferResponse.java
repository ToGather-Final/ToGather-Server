package com.example.pay_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        String transferId,
        Long amount,
        String status,
        LocalDateTime createdAt,
        Long currentBalance
) {
    public static TransferResponse createSuccessResponse(
            UUID transferId,
            Long amount,
            LocalDateTime createdAt,
            Long currentBalance
    ) {
        return new TransferResponse(
                transferId.toString(),
                amount,
                "SUCCESS",
                createdAt,
                currentBalance
        );
    }
}
