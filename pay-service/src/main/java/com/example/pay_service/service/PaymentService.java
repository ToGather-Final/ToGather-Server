package com.example.pay_service.service;

import com.example.pay_service.domain.*;
import com.example.pay_service.dto.PaymentRequest;
import com.example.pay_service.dto.PaymentResponse;
import com.example.pay_service.exception.AccountNotOwnedException;
import com.example.pay_service.exception.InsufficientFundsException;
import com.example.pay_service.exception.PayServiceException;
import com.example.pay_service.repository.IdempotencyKeyRepository;
import com.example.pay_service.repository.PayAccountLedgerRepository;
import com.example.pay_service.repository.PayAccountRepository;
import com.example.pay_service.repository.PaymentRepository;
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
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PayAccountRepository payAccountRepository;
    private final PayAccountLedgerRepository payAccountLedgerRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final PaymentSessionService paymentSessionService;

    @Transactional
    public PaymentResponse executePayment(PaymentRequest request, UUID userId) {
        log.info("결제 실행 시작: payerAccountId={}, amount={}, recipientName={}, userId={}",
                request.payerAccountId(), request.amount(), request.recipientName(), userId);

        if (request.clientRequestId() != null) {
            Optional<Payment> existingPayment = paymentRepository.findByClientRequestId(request.clientRequestId());
            if (existingPayment.isPresent()) {
                log.info("멱등성 재시도: clientRequestId={}", request.clientRequestId());
                return createPaymentResponse(existingPayment.get());
            }
        }

        UUID payerAccountId = UUID.fromString(request.payerAccountId());
        PayAccount payerAccount = payAccountRepository.findByIdAndOwnerUserIdAndIsActiveTrue(payerAccountId, userId)
                .orElseThrow(() -> new AccountNotOwnedException("Account not owned by user"));

        if (!payerAccount.hasSufficientBalance(request.amount())) {
            throw new InsufficientFundsException("Insufficient balance");
        }

        Payment payment = Payment.createDirectPayment(
                payerAccountId,
                request.amount(),
                request.recipientName(),
                request.recipientBankName(),
                request.recipientAccountNumber(),
                request.clientRequestId()
        );

        try {
            payerAccount.debit(request.amount());

            payment.markAsSucceeded();

            paymentRepository.save(payment);
            payAccountRepository.save(payerAccount);

            PayAccountLedger ledgerEntry = PayAccountLedger.createWithRecipient(
                    payerAccountId,
                    TransactionType.PAYMENT,
                    request.amount(),
                    payerAccount.getBalance(),
                    "QR 결제",
                    request.recipientName() // 상점명
            );

            payAccountLedgerRepository.save(ledgerEntry);

            // 멱등성 키 저장
            if (request.clientRequestId() != null) {
                IdempotencyKey idempotencyKey = IdempotencyKey.create(request.clientRequestId(), payerAccountId);
                idempotencyKey.markAsUsed(payment.getId());
                idempotencyKeyRepository.save(idempotencyKey);
            }

            log.info("결제 성공: {}", payment.getPaymentSummary());
            return createPaymentResponse(payment, payerAccount.getBalance());
        } catch (IllegalArgumentException e) {
            throw new InsufficientFundsException("Insufficient balance");
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
    public Page<PaymentResponse> getPaymentHistory(UUID accountId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Payment> paymentPage = paymentRepository.findByPayerAccountIdOrderByCreatedAtDesc(accountId, pageable);
        return paymentPage.map(this::createPaymentResponse);
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
                payment.getRecipientName(), // merchantId 대신 recipientName 사용
                payment.getRecipientBankName(), // "가맹점" 대신 실제 은행명 사용
                payment.getPayerAccountId().toString(),
                payment.getPostedAt(),
                balanceAfter
        );
    }
}