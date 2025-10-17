package com.example.vote_service.controller;

import com.example.vote_service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

/**
 * 실시간 알림 컨트롤러
 * - SSE 연결 및 알림 관리
 */
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "알림 관리", description = "실시간 알림, SSE 연결, 알림 전송 관련 API")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * SSE 스트림 연결
     * GET /notification/stream
     */
    @Operation(summary = "실시간 알림 스트림 연결", description = "SSE를 통해 실시간 알림을 받을 수 있는 스트림에 연결합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 스트림 연결 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 사용자 ID")
    })
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userIdHeader) {
        UUID userId = UUID.fromString(userIdHeader);
        
        // SSE Emitter 생성 (30분 타임아웃)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 사용자 연결 등록
        notificationService.registerUserConnection(userId, emitter);
        
        log.info("🔌 알림 스트림 연결 - userId: {}", userId);
        return emitter;
    }
    
    /**
     * 다른 서비스에서 호출하는 알림 API
     * POST /notification/history
     */
    @Operation(summary = "히스토리 알림 전송", description = "다른 서비스에서 호출하는 히스토리 관련 알림을 전송합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "히스토리 알림 전송 성공"),
        @ApiResponse(responseCode = "500", description = "알림 전송 실패")
    })
    @PostMapping("/history")
    public ResponseEntity<Map<String, Object>> sendHistoryNotification(
            @Parameter(description = "히스토리 알림 요청 데이터", required = true) @RequestBody HistoryNotificationRequest request) {
        
        try {
            notificationService.sendHistoryNotification(
                request.groupId(),
                request.message(),
                request.historyType()
            );
            
            log.info("📤 외부 히스토리 알림 전송 - groupId: {}, type: {}", 
                    request.groupId(), request.historyType());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "알림 전송 완료",
                "groupId", request.groupId(),
                "type", request.historyType()
            ));
            
        } catch (Exception e) {
            log.error("❌ 외부 히스토리 알림 전송 실패 - request: {}, error: {}", request, e.getMessage(), e);
            
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "알림 전송 실패",
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 개별 사용자 알림 전송
     * POST /notification/user
     */
    @Operation(summary = "개별 사용자 알림 전송", description = "특정 사용자에게 개별 알림을 전송합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "개별 사용자 알림 전송 성공"),
        @ApiResponse(responseCode = "500", description = "알림 전송 실패")
    })
    @PostMapping("/user")
    public ResponseEntity<Map<String, Object>> sendUserNotification(
            @Parameter(description = "개별 사용자 알림 요청 데이터", required = true) @RequestBody UserNotificationRequest request) {
        
        try {
            notificationService.sendNotificationToUser(
                request.userId(),
                request.message(),
                request.type()
            );
            
            log.info("📤 개별 사용자 알림 전송 - userId: {}, type: {}", 
                    request.userId(), request.type());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "알림 전송 완료",
                "userId", request.userId(),
                "type", request.type()
            ));
            
        } catch (Exception e) {
            log.error("❌ 개별 사용자 알림 전송 실패 - request: {}, error: {}", request, e.getMessage(), e);
            
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "알림 전송 실패",
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 알림 상태 조회
     * GET /notification/status
     */
    @Operation(summary = "알림 상태 조회", description = "현재 연결된 사용자 수와 알림 시스템 상태를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 상태 조회 성공"),
        @ApiResponse(responseCode = "500", description = "알림 상태 조회 실패")
    })
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getNotificationStatus() {
        try {
            int connectedUsers = notificationService.getConnectedUserCount();
            
            return ResponseEntity.ok(Map.of(
                "connectedUsers", connectedUsers,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("❌ 알림 상태 조회 실패 - error: {}", e.getMessage(), e);
            
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 히스토리 알림 요청 DTO
     */
    public record HistoryNotificationRequest(
        UUID groupId,
        String message,
        String historyType
    ) {}
    
    /**
     * 개별 사용자 알림 요청 DTO
     */
    public record UserNotificationRequest(
        UUID userId,
        String message,
        String type
    ) {}
}
