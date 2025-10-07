package com.example.trading_service.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AccountInfoResponse {
    private UUID accountId;
    private String accountNo;
    private String userId;
    private LocalDateTime createdAt;
    private boolean hasAccount;
}
