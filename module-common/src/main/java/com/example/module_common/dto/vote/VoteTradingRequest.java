package com.example.module_common.dto.vote;

import java.math.BigDecimal;
import java.util.UUID;

public record VoteTradingRequest(
        UUID proposalId,
        UUID groupId,
        UUID stockId,
        TradingAction tradingAction,
        Float quantity,  // Integer → Float 변경 (소수점 거래 지원)
        BigDecimal price,
        String payload,
        Integer totalVotes,
        Integer buyVotes,
        Integer sellVotes,
        Integer holdVotes,
        BigDecimal buyPercentage,
        BigDecimal sellPercentage,
        BigDecimal holdPercentage
) {
}
