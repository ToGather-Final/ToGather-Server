package com.example.module_common.dto.vote;

public record VoteTradingResponse(
        boolean success,
        String message,
        int processedCount
) {
}
