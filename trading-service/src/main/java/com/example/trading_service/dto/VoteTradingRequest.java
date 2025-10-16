package com.example.trading_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class VoteTradingRequest {
    
    @NotNull(message = "투표 ID는 필수입니다.")
    private UUID voteId;
    
    @NotNull(message = "그룹 ID는 필수입니다.")
    private UUID groupId;
    
    @NotNull(message = "주식 ID는 필수입니다.")
    private UUID stockId;
    
    @NotNull(message = "거래 액션은 필수입니다.")
    private TradingAction tradingAction;
    
    @Min(value = 1, message = "주문 수량은 1 이상이어야 합니다.")
    @Max(value = 10000, message = "주문 수량은 10,000 이하여야 합니다.")
    private Integer quantity;
    
    @DecimalMin(value = "0.01", message = "주문 가격은 0.01 이상이어야 합니다.")
    @Max(value = 1000000, message = "주문 가격은 1,000,000 이하여야 합니다.")
    private BigDecimal price;
    
    // 투표 결과 메타데이터
    private Integer totalVotes;           // 총 투표 수
    private Integer buyVotes;             // 매수 투표 수
    private Integer sellVotes;            // 매도 투표 수
    private Integer holdVotes;            // 보유 투표 수
    private BigDecimal buyPercentage;     // 매수 비율
    private BigDecimal sellPercentage;    // 매도 비율
    private BigDecimal holdPercentage;    // 보유 비율
    
    public enum TradingAction {
        BUY,    // 매수
        SELL,   // 매도
        HOLD    // 보유
    }
}




