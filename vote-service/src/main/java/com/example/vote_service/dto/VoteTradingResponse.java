package com.example.vote_service.dto;

public record VoteTradingResponse(
        boolean success,
        String message,
        int processedCount
) {
}
