package com.example.pay_service.service;

import com.example.pay_service.client.TradingServiceClient;
import com.example.pay_service.domain.*;
import com.example.pay_service.dto.FundsTransferRequest;
import com.example.pay_service.dto.FundsTransferResponse;
import com.example.pay_service.dto.TransferRequest;
import com.example.pay_service.dto.TransferResponse;
import com.example.pay_service.exception.PayServiceException;
import com.example.pay_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    private final PayAccountRepository payAccountRepository;
    private final PayAccountLedgerRepository payAccountLedgerRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final TradingServiceClient tradingServiceClient;

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

        PayAccount toAccount = payAccountRepository.findGroupPayAccountByGroupId(groupId)
                .orElseThrow(() -> new PayServiceException("GROUP_PAY_ACCOUNT_NOT_FOUND", "그룹 페이 계좌를 찾을 수 없습니다."));

        Transfer transfer = Transfer.create(
                null,
                toAccount.getId(),
                request.amount(),
                request.clientRequestId()
        );
        transferRepository.save(transfer);

        try {
            FundsTransferRequest fundsTransferRequest = FundsTransferRequest.builder()
                    .txId(UUID.randomUUID())
                    .fromUserId(userId)
                    .toAccountId(toAccount.getId())
                    .amount(request.amount())
                    .transferId(transfer.getId())
                    .clientRequestId(request.clientRequestId())
                    .build();

            FundsTransferResponse response = tradingServiceClient.transferFunds(fundsTransferRequest);

            if (response.isSuccess()) {
                PayAccount updatedToAccount = PayAccount.builder()
                        .id(toAccount.getId())
                        .ownerUserId(toAccount.getOwnerUserId())
                        .balance(toAccount.getBalance() + request.amount())
                        .nickname(toAccount.getNickname())
                        .isActive(toAccount.getIsActive())
                        .groupId(toAccount.getGroupId())
                        .build();

                transfer.markAsSucceeded();

                payAccountRepository.save(updatedToAccount);
                transferRepository.save(transfer);

                PayAccountLedger ledgerEntry = PayAccountLedger.builder()
                        .payAccountId(toAccount.getId())
                        .transactionType(TransactionType.TRANSFER_IN)
                        .amount(request.amount())
                        .balanceAfter(updatedToAccount.getBalance())
                        .description("페이머니 충전")
                        .relatedTransferId(transfer.getId())
                        .build();

                payAccountLedgerRepository.save(ledgerEntry);

                if(request.clientRequestId() != null) {
                    IdempotencyKey idempotencyKey = IdempotencyKey.create(request.clientRequestId(), toAccount.getId());
                    idempotencyKey.markAsUsed(transfer.getId());
                    idempotencyKeyRepository.save(idempotencyKey);
                }

                log.info("페이머니 충전 성공: transferId={}, amount={}", transfer.getId(), request.amount());
                return createTransferResponse(transfer, updatedToAccount.getBalance());
            } else {
                transfer.markAsFailed(response.getFailureReason());
                transferRepository.save(transfer);
                throw new PayServiceException("TRANSFER_FAILED", response.getFailureReason());
            }

        } catch (Exception e) {
            log.error("페이머니 충전 실패: {}", e.getMessage());
            transfer.markAsFailed(e.getMessage());
            transferRepository.save(transfer);
            throw new PayServiceException("TRANSFER_REQUEST_FAILED", "자금 이체 요청 실패: " + e.getMessage());
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
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Transfer> transferPage = transferRepository.findByToAccountIdOrderByCreatedAtDesc(accountId, pageable);

        return transferPage.stream()
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
