package com.example.pay_service.controller;

import com.example.pay_service.dto.PaymentRequest;
import com.example.pay_service.dto.PaymentResponse;
import com.example.pay_service.service.PayAccountService;
import com.example.pay_service.service.PaymentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PayAccountService payAccountService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        log.info("결제 요청: sessionId={}, payerAccountId={}, amount={}, userId={}",
                request.paymentSessionId(), request.payerAccountId(), request.amount(), userId);

        PaymentResponse response = paymentService.executePayment(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        log.info("결제 조회 요청: paymentId={}", paymentId);

        PaymentResponse response = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<PaymentResponse>> getPaymentHistory(
            @RequestParam UUID accountId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal UUID userId){

        log.info("결제 내역 조회 요청: accountId={}, page={}, size={}, userId={}", accountId, page, size, userId);

        if (!payAccountService.isAccountOwnedByUser(accountId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<PaymentResponse> response = paymentService.getPaymentHistory(accountId, page, size);
        return ResponseEntity.ok(response);
    }
}
