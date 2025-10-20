package com.example.pay_service.service;

import com.example.pay_service.domain.PayAccountLedger;
import com.example.pay_service.domain.TransactionType;
import com.example.pay_service.dto.UnifiedHistoryCursorResponse;
import com.example.pay_service.dto.UnifiedHistoryItem;
import com.example.pay_service.repository.PayAccountLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final PayAccountLedgerRepository ledgerRepository;
    private final PayAccountService payAccountService;

    @Transactional(readOnly = true)
    public UnifiedHistoryCursorResponse getUnifiedHistoryCursor(
            UUID accountId,
            UUID userId,
            Integer size,
            String type,
            String cursorCreatedAt,
            Long cursorId
    ) {
        if (!payAccountService.isAccountOwnedByUser(accountId, userId)) {
            throw new IllegalArgumentException("계좌 접근 권한이 없습니다.");
        }

        TransactionType txType = (type == null || type.isBlank()) ? null : TransactionType.valueOf(type);
        PageRequest limit = PageRequest.of(0, size != null & size > 0 ? size : 20);

        List<PayAccountLedger> rows = (cursorCreatedAt == null || cursorId == null)
                ? ledgerRepository.findFirstPage(accountId, txType, limit)
                : ledgerRepository.findNextPageByCursor(
                accountId,
                txType,
                LocalDateTime.parse(cursorCreatedAt),
                cursorId,
                limit
        );

        List<UnifiedHistoryItem> items = rows.stream().map(l -> new UnifiedHistoryItem(
                l.getRelatedPaymentId() != null ? l.getRelatedPaymentId().toString()
                        : (l.getRelatedTransferId() != null ? l.getRelatedTransferId().toString() : null),
                l.getTransactionType().name(),
                l.getAmount(),
                l.getBalanceAfter(),
                l.getDescription(),
                l.getCreatedAt()
        )).toList();

        boolean hasMore = !rows.isEmpty();
        String nextCursorCreatedAt = hasMore ? rows.get(rows.size() - 1).getCreatedAt().toString() : null;
        String nextCursorId = hasMore ? String.valueOf(rows.get(rows.size() - 1).getId()) : null;

        return new UnifiedHistoryCursorResponse(items, nextCursorCreatedAt, nextCursorId, hasMore);
    }
}
