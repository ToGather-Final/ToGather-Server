package com.example.vote_service.dto;

import com.example.vote_service.dto.payload.PayPayload;
import com.example.vote_service.dto.payload.TradePayload;
import com.example.vote_service.model.ProposalAction;
import com.example.vote_service.model.ProposalCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 제안 생성 요청 DTO
 * - groupId는 백엔드에서 사용자의 그룹을 자동으로 조회하여 설정
 * - category에 따라 payload 타입이 달라짐:
 *   - TRADE: TradePayload (stockId, stockName, price, quantity 포함)
 *   - PAY: PayPayload (reason, amountPerPerson 포함)
 */
public record ProposalCreateRequest(
        @NotBlank String proposalName,
        @NotNull ProposalCategory category,
        @NotNull ProposalAction action,
        @NotNull Object payload  // TradePayload 또는 PayPayload
) {
    
    /**
     * TRADE 카테고리인지 확인
     */
    public boolean isTradeCategory() {
        return category == ProposalCategory.TRADE;
    }
    
    /**
     * PAY 카테고리인지 확인
     */
    public boolean isPayCategory() {
        return category == ProposalCategory.PAY;
    }
}
