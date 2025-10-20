package com.example.module_common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentAccountDto {
    private UUID investmentAccountId;
    private UUID userId;
    private String accountNo;
    private LocalDateTime createdAt;
}
