package com.example.pay_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundsTransferResponse {
    private boolean success;
    private UUID txId;
    private UUID transferId;
    private Long amount;
    private LocalDateTime completedAt;
    private String failureReason;
}
