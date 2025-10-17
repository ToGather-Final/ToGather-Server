package com.example.module_common.dto.pay;

import java.time.LocalDateTime;

public record PayRechargeResponse(
        String transferId,
        Long amount,
        String status,
        LocalDateTime createdAt,
        Long currentBalance
) {
}
