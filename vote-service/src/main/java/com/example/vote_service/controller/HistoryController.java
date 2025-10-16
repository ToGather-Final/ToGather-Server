package com.example.vote_service.controller;

import com.example.vote_service.dto.GetHistoryResponse;
import com.example.vote_service.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 히스토리 컨트롤러
 * - 히스토리 조회 API 제공
 */
@Slf4j
@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /**
     * 사용자 히스토리 조회 (페이징)
     * GET /history?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<GetHistoryResponse> getHistory(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            // API Gateway에서 전달받은 사용자 ID 파싱
            UUID userId = UUID.fromString(userIdHeader);
            
            log.info("히스토리 조회 요청 (페이징) - userId: {}, page: {}, size: {}", userId, page, size);
            
            // 히스토리 조회 (페이징)
            GetHistoryResponse response = historyService.getHistoryByUserId(userId, page, size);
            
            log.info("히스토리 조회 완료 (페이징) - userId: {}, items: {}개, nextCursor: {}", 
                    userId, response.items().size(), response.nextCursor());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("히스토리 조회 중 오류 발생 - userIdHeader: {}, error: {}", userIdHeader, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 사용자 히스토리 전체 조회
     * GET /history/all
     */
    @GetMapping("/all")
    public ResponseEntity<GetHistoryResponse> getAllHistory(
            @RequestHeader("X-User-Id") String userIdHeader) {
        
        try {
            // API Gateway에서 전달받은 사용자 ID 파싱
            UUID userId = UUID.fromString(userIdHeader);
            
            log.info("히스토리 전체 조회 요청 - userId: {}", userId);
            
            // 히스토리 전체 조회
            GetHistoryResponse response = historyService.getAllHistoryByUserId(userId);
            
            log.info("히스토리 전체 조회 완료 - userId: {}, items: {}개", 
                    userId, response.items().size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("히스토리 전체 조회 중 오류 발생 - userIdHeader: {}, error: {}", userIdHeader, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
