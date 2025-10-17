package com.example.pay_service.controller;

import com.example.pay_service.dto.PaymentRequest;
import com.example.pay_service.dto.PaymentResponse;
import com.example.pay_service.service.PayAccountService;
import com.example.pay_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "결제 관리", description = "결제 처리, 조회, 내역 관리 관련 API")
public class PaymentController {

    private final PaymentService paymentService;
    private final PayAccountService payAccountService;

    @Operation(summary = "결제 처리", description = "새로운 결제를 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "결제 처리 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "계좌 접근 권한 없음")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Parameter(description = "결제 요청 데이터", required = true) @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UUID userId
    ) {
        log.info("결제 요청: sessionId={}, payerAccountId={}, amount={}, userId={}",
                request.paymentSessionId(), request.payerAccountId(), request.amount(), userId);

        PaymentResponse response = paymentService.executePayment(request, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "결제 내역 조회", description = "특정 결제 ID로 결제 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "결제 내역 조회 성공"),
        @ApiResponse(responseCode = "404", description = "결제 내역을 찾을 수 없음")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "결제 ID", required = true) @PathVariable UUID paymentId) {
        log.info("결제 조회 요청: paymentId={}", paymentId);

        PaymentResponse response = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "결제 내역 목록 조회", description = "사용자의 결제 내역을 페이지네이션하여 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "결제 내역 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "계좌 접근 권한 없음")
    })
    @GetMapping("/history")
    public ResponseEntity<Page<PaymentResponse>> getPaymentHistory(
            @Parameter(description = "계좌 ID", required = true) @RequestParam UUID accountId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기 (1-100)", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal UUID userId){

        log.info("결제 내역 조회 요청: accountId={}, page={}, size={}, userId={}", accountId, page, size, userId);

        if (!payAccountService.isAccountOwnedByUser(accountId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Page<PaymentResponse> response = paymentService.getPaymentHistory(accountId, page, size);
        return ResponseEntity.ok(response);
    }
}
