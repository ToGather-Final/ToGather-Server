package com.example.pay_service.controller;

import com.example.module_common.dto.pay.PayRechargeRequest;
import com.example.module_common.dto.pay.PayRechargeResponse;
import com.example.pay_service.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "이체 관리", description = "페이머니 충전, 이체 내역 조회 관련 API")
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "페이머니 충전", description = "그룹 페이머니를 충전합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "페이머니 충전 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/recharge")
    public ResponseEntity<PayRechargeResponse> rechargePayMoney(
            @Parameter(description = "충전 요청 데이터", required = true) @Valid @RequestBody PayRechargeRequest request,
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "그룹 ID", required = true) @RequestHeader("X-Group-Id") UUID groupId
    ) {
        log.info("페이머니 충전 요청: amount={}, userId={}, groupId={}", request.amount(), userId, groupId);

        PayRechargeResponse response = transferService.executeTransfer(request, userId, groupId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "이체 내역 조회", description = "특정 이체 ID로 이체 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이체 내역 조회 성공"),
        @ApiResponse(responseCode = "404", description = "이체 내역을 찾을 수 없음")
    })
    @GetMapping("/{transferId}")
    public ResponseEntity<PayRechargeResponse> getTransfer(
            @Parameter(description = "이체 ID", required = true) @PathVariable UUID transferId) {
        log.info("송금 조회 요청: transferId={}", transferId);

        PayRechargeResponse response = transferService.getTransfer(transferId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이체 내역 목록 조회", description = "사용자의 이체 내역을 페이지네이션하여 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이체 내역 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/history")
    public ResponseEntity<List<PayRechargeResponse>> getTransferHistory(
            @Parameter(description = "계좌 ID", required = true) @RequestParam UUID accountId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UUID userId
    ) {
        log.info("송금 내역 조회 요청: accountId={}, page={}, size={}, userId={}", accountId, page, size, userId);

        List<PayRechargeResponse> response = transferService.getTransferHistory(accountId, page, size);
        return ResponseEntity.ok(response);
    }
}
