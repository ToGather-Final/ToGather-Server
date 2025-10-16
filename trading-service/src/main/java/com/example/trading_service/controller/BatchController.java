package com.example.trading_service.controller;

import com.example.trading_service.dto.ApiResponse;
import com.example.trading_service.service.BatchProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {

    private final BatchProcessingService batchProcessingService;

    /**
     * 수동 배치 처리 실행 (관리자용)
     */
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<String>> executeBatch() {
        log.info("수동 배치 처리 요청");
        
        try {
            batchProcessingService.executeManualBatch();
            return ResponseEntity.ok(ApiResponse.success("배치 처리가 성공적으로 완료되었습니다."));
        } catch (Exception e) {
            log.error("수동 배치 처리 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("배치 처리 중 오류가 발생했습니다: " + e.getMessage(), "BATCH_PROCESSING_FAILED")
            );
        }
    }

    /**
     * 캐싱 테이블 초기화 (관리자용)
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<ApiResponse<String>> clearCacheTables() {
        log.info("캐싱 테이블 초기화 요청");
        
        try {
            batchProcessingService.clearCacheTables();
            return ResponseEntity.ok(ApiResponse.success("캐싱 테이블이 성공적으로 초기화되었습니다."));
        } catch (Exception e) {
            log.error("캐싱 테이블 초기화 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("캐싱 테이블 초기화 중 오류가 발생했습니다: " + e.getMessage(), "CACHE_CLEAR_FAILED")
            );
        }
    }

    /**
     * 배치 처리 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<String>> getBatchStatus() {
        return ResponseEntity.ok(ApiResponse.success("배치 처리 서비스가 정상적으로 실행 중입니다."));
    }
}
