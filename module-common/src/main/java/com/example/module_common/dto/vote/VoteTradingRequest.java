package com.example.module_common.dto.vote;

import java.math.BigDecimal;
import java.util.UUID;

public record VoteTradingRequest(
        UUID proposalId,
        UUID groupId,
        UUID stockId,
        TradingAction tradingAction,
        Integer quantity,
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
