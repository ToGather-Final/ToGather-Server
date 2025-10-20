package com.example.module_common.dto;

public record TransferToPayResponse(
        String status,
        String message,
        Long balanceAfter
) {
    public static TransferToPayResponse success(Long balanceAfter) {
        return new TransferToPayResponse("SUCCESS", "송금 성공", balanceAfter);
    }

    public static TransferToPayResponse failure(String message) {
        return new TransferToPayResponse("FAILURE", message, 0L);
    }
}
