package com.example.trading_service.controller;

import com.example.module_common.dto.vote.VoteTradingRequest;
import com.example.module_common.dto.vote.VoteTradingResponse;
import com.example.trading_service.dto.ApiResponse;
import com.example.trading_service.service.VoteTradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vote-trading")
@RequiredArgsConstructor
@Slf4j
public class VoteTradingController {

    private final VoteTradingService voteTradingService;

    /**
     * 투표 결과에 따른 그룹 거래 실행
     * Vote-Service에서 호출하는 API
     */
    @PostMapping("/execute")
    public ResponseEntity<VoteTradingResponse> executeVoteBasedTrading(
            @Valid @RequestBody VoteTradingRequest request) {
        
        log.info("투표 기반 거래 실행 요청 - 투표ID: {}, 그룹ID: {}, 액션: {}", 
                request.proposalId(), request.groupId(), request.tradingAction());

        try {
            // 투표 결과 검증
            if (!voteTradingService.validateVoteResult(request)) {
                return ResponseEntity.badRequest().body(
                        new VoteTradingResponse(false, "투표 결과가 유효하지 않습니다.", 0)
                );
            }

            // 투표 기반 거래 실행
            int processedCount = voteTradingService.executeVoteBasedTrading(request);

            return ResponseEntity.ok(new VoteTradingResponse(
                    true,
                    String.format("투표 기반 거래가 완료되었습니다. 처리된 거래 수: %d", processedCount),
                    processedCount
            ));

        } catch (Exception e) {
            log.error("투표 기반 거래 실행 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    new VoteTradingResponse(false, e.getMessage(), 0)
            );
        }
    }

    /**
     * 투표 결과 검증
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateVoteResult(
            @Valid @RequestBody VoteTradingRequest request) {
        
        try {
            boolean isValid = voteTradingService.validateVoteResult(request);
            return ResponseEntity.ok(ApiResponse.success(
                    isValid ? "투표 결과가 유효합니다." : "투표 결과가 유효하지 않습니다.",
                    isValid
            ));
        } catch (Exception e) {
            log.error("투표 결과 검증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(e.getMessage(), "VOTE_VALIDATION_FAILED")
            );
        }
    }
}
