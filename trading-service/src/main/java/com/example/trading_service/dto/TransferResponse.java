package com.example.trading_service.dto;

import java.util.UUID;

public record TransferResponse(
        UUID transferId,
        String status,
        String message
) {
}
