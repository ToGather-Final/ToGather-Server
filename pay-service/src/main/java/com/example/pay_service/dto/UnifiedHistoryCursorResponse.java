package com.example.pay_service.dto;

import java.util.List;

public record UnifiedHistoryCursorResponse(
        List<UnifiedHistoryItem> items,
        String nextCursorCreatedAt,
        String nextCursorId,
        boolean hasMore
) {
}
