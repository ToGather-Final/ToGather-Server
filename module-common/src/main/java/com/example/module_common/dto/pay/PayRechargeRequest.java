package com.example.module_common.dto.pay;

public record PayRechargeRequest(
        Long amount,
        String clientRequestId
) {
}
