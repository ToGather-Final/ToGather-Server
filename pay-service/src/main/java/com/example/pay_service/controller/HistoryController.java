package com.example.pay_service.controller;

import com.example.pay_service.dto.UnifiedHistoryCursorResponse;
import com.example.pay_service.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/pay/history")
@RequiredArgsConstructor
@Slf4j
public class HistoryController {

    private final HistoryService historyService;

    @Operation(summary = "통합 히스토리(무한 스크롤) 조회", description = "최신순으로 결제/이체 내역을 커서 기반으로 조회합니다.")
    @GetMapping("/cursor")
    public ResponseEntity<UnifiedHistoryCursorResponse> getUnifiedHistoryCursor(
            @Parameter(description = "계좌 ID", required = true) @RequestParam UUID accountId,
            @Parameter(description = "한 번에 가져올 개수", example = "20") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "거래 타입 필터", example = "PAYMENT") @RequestParam(required = false) String type,
            @Parameter(description = "다음 페이지 커서: createdAt(ISO8601)") @RequestParam(required = false) String cursorCreatedAt,
            @Parameter(description = "다음 페이지 커서: ledger PK(id)") @RequestParam(required = false) Long cursorId,
            @AuthenticationPrincipal UUID userId
    ) {
        log.info("통합 히스토리(커서) 조회: accountId={}, size={}, type={}, cursorCreatedAt={}, cursorId={}, userId={}",
                accountId, size, type, cursorCreatedAt, cursorId, userId);

        UnifiedHistoryCursorResponse res =
                historyService.getUnifiedHistoryCursor(accountId, userId, size, type, cursorCreatedAt, cursorId);
        return ResponseEntity.ok(res);
    }
}
