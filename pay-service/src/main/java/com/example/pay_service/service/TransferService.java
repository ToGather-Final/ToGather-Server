package com.example.pay_service.service;

import com.example.pay_service.domain.*;
import com.example.pay_service.dto.TransferRequest;
import com.example.pay_service.dto.TransferResponse;
import com.example.pay_service.exception.InsufficientFundsException;
import com.example.pay_service.exception.PayServiceException;
import com.example.pay_service.repository.AccountRepository;
import com.example.pay_service.repository.IdempotencyKeyRepository;
import com.example.pay_service.repository.LedgerEntryRepository;
import com.example.pay_service.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Transactional
    public TransferResponse executeTransfer(TransferRequest request, UUID userId, UUID groupId) {
        log.info("페이머니 충전 시작: amount={}, userId={}, groupId={}", request.amount(), userId, groupId);

        if (request.clientRequestId() != null) {
            Optional<Transfer> existingTransfer = transferRepository.findByClientRequestId(request.clientRequestId());
            if (existingTransfer.isPresent()) {
                log.info("멱등성 재시도: clientRequestId={}", request.clientRequestId());
                return createTransferResponse(existingTransfer.get());
            }
        }

        List<Account> userCmaAccounts = accountRepository.findByOwnerUserIdAndTypeAndIsActiveTrue(userId, AccountType.CMA);
        if (userCmaAccounts.isEmpty()) {
            throw new PayServiceException("CMA_ACCOUNT_NOT_FOUND", "CMA 계좌를 찾을 수 없습니다.");
        }
        Account fromAccount = userCmaAccounts.get(0);

        Account toAccount = accountRepository.findGroupPayAccountByGroupId(groupId)
                .orElseThrow(() -> new PayServiceException("GROUP_PAY_ACCOUNT_NOT_FOUND", "그룹 페이 계좌를 찾을 수 없습니다."));

        if (!fromAccount.hasSufficientBalance(request.amount())) {
            throw new InsufficientFundsException("잔액이 부족합니다.");
        }

        Transfer transfer = Transfer.create(
                fromAccount.getId(),
                toAccount.getId(),
                request.amount(),
                request.clientRequestId()
        );

        try {
            UUID txId = UUID.randomUUID();
            LedgerEntry debitEntry = LedgerEntry.createDebitEntry(
                    txId,
                    fromAccount.getId(),
                    request.amount(),
                    TxType.TRANSFER,
                    transfer.getId(),
                    "PAY_MONEY_RECHARGE"
            );

            LedgerEntry creditEntry = LedgerEntry.createCreditEntry(
                    txId,
                    toAccount.getId(),
                    request.amount(),
                    TxType.TRANSFER,
                    transfer.getId(),
                    "PAY_MONEY_RECHARGE"
            );

            fromAccount.debit(request.amount());
            toAccount.credit(request.amount());

            transfer.markAsSucceeded();
            debitEntry.markAsCompleted();
            creditEntry.markAsCompleted();

            transferRepository.save(transfer);
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);
            ledgerEntryRepository.save(debitEntry);
            ledgerEntryRepository.save(creditEntry);

            if (request.clientRequestId() != null) {
                IdempotencyKey idempotencyKey = IdempotencyKey.create(request.clientRequestId(), fromAccount.getId());
                idempotencyKey.markAsUsed(transfer.getId());
                idempotencyKeyRepository.save(idempotencyKey);
            }

            log.info("페이머니 충전 성공: transferId={}, amount={}", transfer.getId(), request.amount());
            return createTransferResponse(transfer, toAccount.getBalance());
        } catch (Exception e) {
            log.error("페이머니 충전 실패: {}", e.getMessage());
            transfer.markAsFailed(e.getMessage());
            transferRepository.save(transfer);
            throw new PayServiceException("RECHARGE_FAILED", "페이머니 충전 실퍠: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(UUID transferId) {
        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new PayServiceException("TRANSFER_NOT_FOUND", "송금 내역을 찾을 수 없습니다"));

        return createTransferResponse(transfer);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getTransferHistory(UUID accountId, int page, int size) {
        List<Transfer> transfers = transferRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        return transfers.stream()
                .map(this::createTransferResponse)
                .toList();
    }

    private TransferResponse createTransferResponse(Transfer transfer) {
        return createTransferResponse(transfer, null);
    }

    private TransferResponse createTransferResponse(Transfer transfer, Long currentBalance) {
        return TransferResponse.createSuccessResponse(
                transfer.getId(),
                transfer.getAmount(),
                transfer.getCreatedAt(),
                currentBalance
        );
    }
}
