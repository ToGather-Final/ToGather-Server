package com.example.pay_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundsTransferRequest {
    private UUID txId;
    private UUID fromUserId;
    private UUID toAccountId;
    private Long amount;
    private UUID transferId;
    private String clientRequestId;
}
