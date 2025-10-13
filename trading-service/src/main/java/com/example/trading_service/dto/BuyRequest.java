package com.example.trading_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BuyRequest {
    
    @NotNull(message = "주식 ID는 필수입니다.")
    private UUID stockId;
    
    @NotNull(message = "수량은 필수입니다.")
    @Positive(message = "수량은 양수여야 합니다.")
    private Integer quantity;
    
    @NotNull(message = "가격은 필수입니다.")
    @Positive(message = "가격은 양수여야 합니다.")
    private Float price;
    
    // 시장가 주문 여부 (true: 시장가, false: 지정가)
    private Boolean isMarketOrder = false;
}


