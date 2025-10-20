package com.example.pay_service.dto;

public record PayRechargeRequest(
        Long amount,
        String clientRequestId
) {
}
