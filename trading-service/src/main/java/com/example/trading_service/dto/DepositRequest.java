package com.example.trading_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {
    
    @NotNull(message = "충전 금액은 필수입니다.")
    @Min(value = 1000, message = "최소 충전 금액은 1,000원입니다.")
    @Max(value = 10000000, message = "최대 충전 금액은 10,000,000원입니다.")
    private BigDecimal amount;
}


