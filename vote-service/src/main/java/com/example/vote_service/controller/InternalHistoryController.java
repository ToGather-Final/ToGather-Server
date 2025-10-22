package com.example.vote_service.controller;

import com.example.vote_service.dto.TradeFailedHistoryRequest;
import com.example.vote_service.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 히스토리 API 컨트롤러
 * - 다른 서비스에서 히스토리 저장을 요청할 때 사용
 */
@Slf4j
@RestController
@RequestMapping("/internal/history")
@RequiredArgsConstructor
@Tag(name = "내부 히스토리", description = "서비스 간 히스토리 저장 관련 API")
public class InternalHistoryController {

    private final HistoryService historyService;

    /**
     * 거래 실패 히스토리 저장
     * POST /internal/history/trade-failed
     */
    @Operation(summary = "거래 실패 히스토리 저장", description = "거래 실패 시 히스토리를 저장합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "거래 실패 히스토리 저장 성공"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/trade-failed")
    public ResponseEntity<Void> saveTradeFailedHistory(@RequestBody TradeFailedHistoryRequest request) {
        try {
            log.info("거래 실패 히스토리 저장 요청 - 사용자: {}, 종목: {}, 사유: {}", 
                    request.getUserId(), request.getStockName(), request.getReason());

            historyService.saveTradeFailedHistory(request);

            log.info("거래 실패 히스토리 저장 완료 - 사용자: {}, 종목: {}", 
                    request.getUserId(), request.getStockName());

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("거래 실패 히스토리 저장 실패 - 사용자: {}, 종목: {}, 사유: {} - {}", 
                    request.getUserId(), request.getStockName(), request.getReason(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
