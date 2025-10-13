package com.example.trading_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {
    
    @NotNull(message = "충전 금액은 필수입니다.")
    @Positive(message = "충전 금액은 양수여야 합니다.")
    private Long amount;
}


