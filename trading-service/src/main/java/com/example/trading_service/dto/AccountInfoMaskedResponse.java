package com.example.trading_service.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AccountInfoMaskedResponse {
    private UUID accountId;
    private String accountNo; // 마스킹된 계좌번호 (예: 352-****-****-99)
    private String userId;
    private LocalDateTime createdAt;
    private boolean hasAccount;
}


