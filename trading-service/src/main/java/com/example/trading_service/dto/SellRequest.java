package com.example.trading_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SellRequest {
    
    // stockId 또는 stockCode 중 하나는 필수
    private UUID stockId;
    private String stockCode;
    
    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    @Max(value = 10000, message = "수량은 10,000 이하여야 합니다.")
    private Integer quantity;
    
    @NotNull(message = "가격은 필수입니다.")
    @DecimalMin(value = "0.01", message = "가격은 0.01 이상이어야 합니다.")
    private BigDecimal price;
    
    // 시장가 주문 여부 (true: 시장가, false: 지정가)
    private Boolean isMarketOrder = false;
}


