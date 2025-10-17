package com.example.trading_service.controller;

import com.example.trading_service.dto.OrderBookResponse;
import com.example.trading_service.service.KisWebSocketClient;
import com.example.trading_service.service.WebSocketOrderBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebSocketTestController {

    private final KisWebSocketClient kisWebSocketClient;
    private final WebSocketOrderBookService webSocketOrderBookService;

    /**
     * WebSocket 연결 상태 확인
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWebSocketStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("connected", kisWebSocketClient.isConnected());
        status.put("subscribedStocks", kisWebSocketClient.getSubscribedStocks());
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }

    /**
     * WebSocket 재연결
     */
    @GetMapping("/reconnect")
    public ResponseEntity<Map<String, Object>> reconnectWebSocket() {
        try {
            kisWebSocketClient.connect();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "WebSocket 재연결 시도 완료");
            response.put("connected", kisWebSocketClient.isConnected());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "WebSocket 재연결 실패: " + e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * approval_key 발급 테스트
     */
    @GetMapping("/test-approval-key")
    public ResponseEntity<Map<String, Object>> testApprovalKey() {
        try {
            // KisTokenService를 직접 주입받아서 테스트
            // 이 부분은 실제로는 KisTokenService를 주입받아야 함
            Map<String, Object> response = new HashMap<>();
            response.put("status", "info");
            response.put("message", "approval_key 발급 테스트는 별도로 구현 필요");
            response.put("note", "한투 API 문서에서 정확한 요청 형식 확인 필요");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "테스트 실패: " + e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * WebSocket 캐시 상태 확인
     */
    @GetMapping("/cache-status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "WebSocket 캐시 상태 조회 완료");
            response.put("data", webSocketOrderBookService.getCacheStatus());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "캐시 상태 조회 실패: " + e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 특정 주식의 캐시된 호가 데이터 조회 (WebSocket 우선 테스트)
     */
    @GetMapping("/cached-orderbook/{stockCode}")
    public ResponseEntity<Map<String, Object>> getCachedOrderBook(@PathVariable String stockCode) {
        try {
            OrderBookResponse cachedData = webSocketOrderBookService.getCachedOrderBook(stockCode);
            
            Map<String, Object> response = new HashMap<>();
            if (cachedData != null) {
                response.put("status", "success");
                response.put("message", "WebSocket 캐시에서 호가 데이터 조회 성공");
                response.put("data", cachedData);
                response.put("source", "websocket_cache");
            } else {
                response.put("status", "info");
                response.put("message", "WebSocket 캐시에 데이터 없음 - REST API 폴백 필요");
                response.put("data", null);
                response.put("source", "cache_miss");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "캐시된 호가 데이터 조회 실패: " + e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }
}
