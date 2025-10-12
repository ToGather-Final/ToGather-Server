package com.example.pay_service.service;

import com.example.pay_service.domain.*;
import com.example.pay_service.dto.PaymentRequest;
import com.example.pay_service.dto.PaymentResponse;
import com.example.pay_service.exception.AccountNotOwnedException;
import com.example.pay_service.exception.InsufficientFundsException;
import com.example.pay_service.exception.PayServiceException;
import com.example.pay_service.repository.AccountRepository;
import com.example.pay_service.repository.IdempotencyKeyRepository;
import com.example.pay_service.repository.LedgerEntryRepository;
import com.example.pay_service.repository.PaymentRepository;
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
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final PaymentSessionService paymentSessionService;

    @Transactional
    public PaymentResponse executePayment(PaymentRequest request, UUID userId) {
        log.info("결제 실행 시작: sessionId={}, payerAccountId={}, amount={}, userId={}",
                request.paymentSessionId(), request.payerAccountId(), request.amount(), userId);

        if (request.clientRequestId() != null) {
            Optional<Payment> existingPayment = paymentRepository.findByClientRequestId(request.clientRequestId());
            if (existingPayment.isPresent()) {
                log.info("멱등성 재시도: clientRequestId={}", request.clientRequestId());
                return createPaymentResponse(existingPayment.get());
            }
        }

        PaymentSession session = paymentSessionService.getSessionOrThrow(request.paymentSessionId());
        if (session.isUsed()){
            throw new PayServiceException("SESSION_USED", "Payment session already used");
        }

        UUID payerAccountId = UUID.fromString(request.payerAccountId());
        Account payerAccount = accountRepository.findByIdAndOwnerUserIdAndIsActiveTrue(payerAccountId, userId)
                .orElseThrow(() -> new AccountNotOwnedException("Account not owned by user"));

        if (!payerAccount.hasSufficientBalance(request.amount())) {
            throw new InsufficientFundsException("Insufficient balance");
        }

        Account merchantAccount = accountRepository.findByIdAndIsActiveTrue(session.getMerchantAccountId())
                .orElseThrow(() -> new PayServiceException("MERCHANT_ACCOUNT_NOT_FOUND", "Merchant account not found"));

        Payment payment = Payment.create(
                request.paymentSessionId(),
                payerAccountId,
                session.getMerchantId(),
                request.amount(),
                request.clientRequestId()
        );

        try {
            UUID txId = UUID.randomUUID();
            LedgerEntry debitEntry = LedgerEntry.createDebitEntry(
                    txId,
                    payerAccountId,
                    request.amount(),
                    TxType.PAYMENT,
                    payment.getId(),
                    "PAYMENT:" + request.paymentSessionId()
            );

            LedgerEntry creditEntry = LedgerEntry.createCreditEntry(
                    txId,
                    session.getMerchantAccountId(),
                    request.amount(),
                    TxType.PAYMENT,
                    payment.getId(),
                    "PAYMENT:" + request.paymentSessionId()
            );

            payerAccount.debit(request.amount());
            merchantAccount.credit(request.amount());

            payment.markAsSucceeded();

            paymentRepository.save(payment);
            accountRepository.save(payerAccount);
            accountRepository.save(merchantAccount);
            ledgerEntryRepository.save(debitEntry);
            ledgerEntryRepository.save(creditEntry);

            if (request.clientRequestId() != null) {
                IdempotencyKey idempotencyKey = IdempotencyKey.create(request.clientRequestId(), payerAccountId);
                idempotencyKey.markAsUsed(payment.getId());
                idempotencyKeyRepository.save(idempotencyKey);
            }

            paymentSessionService.markAsUsed(request.paymentSessionId());

            log.info("결제 성공: paymentId={},amount={}", payment.getId(), request.amount());
            return createPaymentResponse(payment, payerAccount.getBalance());
        } catch (Exception e) {
            log.error("결제 실패: {}", e.getMessage());
            payment.markAsFailed(e.getMessage());
            paymentRepository.save(payment);
            throw new PayServiceException("PAYMENT_FAILED", "Payment processing failed: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PayServiceException("PAYMENT_NOT_FOUND", "Payment not found"));

        return createPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistory(UUID accountId, int page, int size) {
        List<Payment> payments = paymentRepository.findByPayerAccountIdOrderByCreatedAtDesc(accountId);
        return payments.stream()
                .map(this::createPaymentResponse)
                .toList();
    }

    private PaymentResponse createPaymentResponse(Payment payment) {
        return createPaymentResponse(payment, null);
    }

    private PaymentResponse createPaymentResponse(Payment payment, Long balanceAfter) {
        return PaymentResponse.createSuccessResponse(
                payment.getId().toString(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMerchantId().toString(),
                "가맹점",
                payment.getPayerAccountId().toString(),
                payment.getPostedAt(),
                balanceAfter
        );
    }
}
