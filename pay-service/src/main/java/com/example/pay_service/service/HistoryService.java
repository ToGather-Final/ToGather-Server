package com.example.pay_service.service;

import com.example.pay_service.domain.PayAccount;
import com.example.pay_service.domain.PayAccountLedger;
import com.example.pay_service.domain.TransactionType;
import com.example.pay_service.dto.UnifiedHistoryCursorResponse;
import com.example.pay_service.dto.UnifiedHistoryItem;
import com.example.pay_service.repository.PayAccountLedgerRepository;
import com.example.pay_service.repository.PayAccountRepository;
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
    private final PayAccountRepository payAccountRepository;

    @Transactional(readOnly = true)
    public UnifiedHistoryCursorResponse getUnifiedHistoryCursor(
            UUID accountId,
            UUID userId,
            Integer size,
            String type,
            String cursorCreatedAt,
            Long cursorId
    ) {
        // 1. 계좌 정보 조회
        PayAccount account = payAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));

        // 2. 권한 확인
        boolean isAccountOwner = account.getOwnerUserId().equals(userId);
        boolean isGroupLeader = payAccountService.isGroupLeader(account.getGroupId(), userId);
        boolean isGroupMember = payAccountService.isGroupMember(account.getGroupId(), userId);

        if (!isAccountOwner && !isGroupLeader && !isGroupMember) {
            throw new IllegalArgumentException("계좌 접근 권한이 없습니다.");
        }

        // 3. 거래 타입 필터링
        TransactionType txType = (type == null || type.isBlank()) ? null : TransactionType.valueOf(type);
        PageRequest limit = PageRequest.of(0, size != null && size > 0 ? size : 20);

        // 4. 데이터 조회
        List<PayAccountLedger> rows = (cursorCreatedAt == null || cursorId == null)
                ? ledgerRepository.findFirstPage(accountId, txType, limit)
                : ledgerRepository.findNextPageByCursor(
                accountId,
                txType,
                LocalDateTime.parse(cursorCreatedAt),
                cursorId,
                limit
        );

        // 5. 사용자별 맞춤 변환
        List<UnifiedHistoryItem> items = rows.stream()
                .map(l -> convertToHistoryItem(l, userId, isAccountOwner, isGroupLeader))
                .toList();

        // 6. 응답 생성
        boolean hasMore = !rows.isEmpty();
        String nextCursorCreatedAt = hasMore ? rows.get(rows.size() - 1).getCreatedAt().toString() : null;
        String nextCursorId = hasMore ? String.valueOf(rows.get(rows.size() - 1).getId()) : null;

        return new UnifiedHistoryCursorResponse(items, nextCursorCreatedAt, nextCursorId, hasMore);
    }

    private UnifiedHistoryItem convertToHistoryItem(PayAccountLedger ledger, UUID userId, boolean isAccountOwner, boolean isGroupLeader) {
        String id = ledger.getRelatedPaymentId() != null
                ? ledger.getRelatedPaymentId().toString()
                : (ledger.getRelatedTransferId() != null ? ledger.getRelatedTransferId().toString() : null);

        String type = ledger.getTransactionType().name();
        long amount = ledger.getAmount();
        String description = ledger.getDescription();

        // 사용자별 맞춤 변환
        if (ledger.getTransactionType() == TransactionType.TRANSFER_IN) {
            // 입금: "김철수가 50,000원 입금"
            String payerName = ledger.getPayerName() != null ? ledger.getPayerName() : "알 수 없는 사용자";
            description = payerName + String.format("%,d원 입금", amount);
        } else if (ledger.getTransactionType() == TransactionType.PAYMENT) {
            // 결제: "김철수가 스타벅스에서 5,000원 결제"
            String recipientName = ledger.getRecipientName() != null ? ledger.getRecipientName() : "알 수 없는 상점";
            description = recipientName + "에서 " + String.format("%,d원 결제", amount);
        }

        return new UnifiedHistoryItem(
                id,
                type,
                amount,
                ledger.getBalanceAfter(),
                description,
                ledger.getCreatedAt()
        );
    }
}
