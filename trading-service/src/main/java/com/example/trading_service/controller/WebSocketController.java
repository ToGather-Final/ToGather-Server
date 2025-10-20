package com.example.trading_service.controller;

import com.example.trading_service.service.KisWebSocketClient;
import com.example.trading_service.service.RedisCacheService;
import com.example.trading_service.service.OrderBookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final KisWebSocketClient kisWebSocketClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisCacheService redisCacheService;
    private final OrderBookService orderBookService;
    private final ObjectMapper objectMapper;

    /**
     * 호가 구독 요청
     */
    @MessageMapping("/orderbook/subscribe")
    public void subscribeOrderBook(String message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("📡 호가 구독 요청: {}", message);
        
        String stockCode = null; // 변수 스코프를 메서드 레벨로 이동
        
        try {
            // JSON 메시지에서 stockCode 추출
            try {
                JsonNode jsonNode = objectMapper.readTree(message);
                stockCode = jsonNode.get("stockCode").asText();
                log.info("📊 추출된 종목코드: {}", stockCode);
            } catch (Exception e) {
                // JSON 파싱 실패 시 메시지 자체를 stockCode로 사용
                stockCode = message;
                log.info("📊 JSON 파싱 실패, 메시지 자체를 종목코드로 사용: {}", stockCode);
            }
            
            // 1. 캐시된 호가 데이터 조회
            Object cachedOrderBook = redisCacheService.getCachedWebSocketOrderBook(stockCode);
            
            if (cachedOrderBook != null) {
                // 캐시된 데이터가 있으면 즉시 전송
                messagingTemplate.convertAndSend("/topic/orderbook/" + stockCode, cachedOrderBook);
                log.info("✅ 캐시된 호가 데이터 전송: {}", stockCode);
            } else {
                // 캐시된 데이터가 없음 - OrderBookService에서 REST API 폴백 처리
                log.info("📊 캐시된 데이터 없음 - OrderBookService에서 REST API 폴백 처리: {}", stockCode);
                try {
                    // OrderBookService.getOrderBook() 호출 (REST API 폴백 포함)
                    Object orderBookData = orderBookService.getOrderBook(stockCode);
                    if (orderBookData != null) {
                        messagingTemplate.convertAndSend("/topic/orderbook/" + stockCode, orderBookData);
                        log.info("✅ REST API 폴백 호가 데이터 전송: {}", stockCode);
                    } else {
                        log.warn("⚠️ REST API 폴백에서도 호가 데이터를 찾을 수 없음: {}", stockCode);
                    }
                } catch (Exception e) {
                    log.error("❌ REST API 폴백 호가 데이터 조회 실패: {} - {}", stockCode, e.getMessage());
                }
            }
            
            // 2. 구독 상태 응답
            Map<String, Object> statusResponse = new HashMap<>();
            statusResponse.put("action", "subscribe");
            statusResponse.put("stockCode", stockCode);
            statusResponse.put("status", "success");
            statusResponse.put("message", "호가 구독이 시작되었습니다");
            statusResponse.put("hasCachedData", cachedOrderBook != null);
            
            messagingTemplate.convertAndSend("/topic/orderbook/status", statusResponse);
            
        } catch (Exception e) {
            log.error("❌ 호가 구독 처리 실패: {} - {}", stockCode != null ? stockCode : "unknown", e.getMessage());
            
            // 에러 응답
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("action", "subscribe");
            errorResponse.put("stockCode", stockCode != null ? stockCode : "unknown");
            errorResponse.put("status", "error");
            errorResponse.put("message", "호가 구독 처리 중 오류 발생: " + e.getMessage());
            
            messagingTemplate.convertAndSend("/topic/orderbook/status", errorResponse);
        }
    }

    /**
     * 호가 구독 해제 요청
     */
    @MessageMapping("/orderbook/unsubscribe")
    @SendTo("/topic/orderbook/status")
    public Map<String, Object> unsubscribeOrderBook(String stockCode, SimpMessageHeaderAccessor headerAccessor) {
        log.info("호가 구독 해제 요청: {}", stockCode);
        
        Map<String, Object> response = new HashMap<>();
        response.put("action", "unsubscribe");
        response.put("stockCode", stockCode);
        response.put("status", "success");
        response.put("message", "호가 구독이 해제되었습니다");
        
        return response;
    }

    /**
     * WebSocket 연결 상태 조회
     */
    @MessageMapping("/status")
    @SendTo("/topic/status")
    public Map<String, Object> getConnectionStatus(SimpMessageHeaderAccessor headerAccessor) {
        log.info("연결 상태 조회 요청");
        
        Map<String, Object> response = new HashMap<>();
        response.put("connected", kisWebSocketClient.isConnected());
        response.put("subscribedStocks", kisWebSocketClient.getSubscribedStocks());
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    /**
     * 모든 주식 구독 요청
     */
    @MessageMapping("/orderbook/subscribe-all")
    @SendTo("/topic/orderbook/status")
    public Map<String, Object> subscribeAllStocks(SimpMessageHeaderAccessor headerAccessor) {
        log.info("모든 주식 구독 요청");
        
        List<String> subscribedStocks = kisWebSocketClient.getSubscribedStocks();
        
        Map<String, Object> response = new HashMap<>();
        response.put("action", "subscribe-all");
        response.put("status", "success");
        response.put("subscribedStocks", subscribedStocks);
        response.put("message", "15개 주식 호가 구독이 시작되었습니다");
        
        return response;
    }

    /**
     * 웹소켓 연결 상태 모니터링 및 자동 재연결 (30초마다)
     * 주기적으로 모니터링하되, 이미 연결 중이면 재연결하지 않음
     */
    @Scheduled(fixedRate = 30000)
    public void monitorWebSocketConnection() {
        try {
            // 이미 연결되어 있으면 모니터링만 하고 재연결하지 않음
            if (kisWebSocketClient.isConnected()) {
                log.debug("✅ 웹소켓 연결 상태 양호");
                return;
            }
            
            // 연결이 끊어진 경우에만 재연결 시도
            if (!kisWebSocketClient.isHealthy()) {
                log.warn("⚠️ 웹소켓 연결 상태 불량 - 재연결 시도");
                kisWebSocketClient.checkAndReconnect();
            }
        } catch (Exception e) {
            log.error("웹소켓 연결 모니터링 중 오류 발생", e);
        }
    }

    /**
     * 누락된 주식 구독 확인 및 추가 (30초마다)
     */
    @Scheduled(fixedRate = 30000)
    public void ensureAllStocksSubscribed() {
        try {
            if (kisWebSocketClient.isHealthy()) {
                kisWebSocketClient.ensureAllStocksSubscribed();
            }
        } catch (Exception e) {
            log.error("누락된 주식 구독 확인 중 오류 발생", e);
        }
    }

    /**
     * 테스트용 주기적 메시지 브로드캐스트 (5초마다)
     */
    @Scheduled(fixedRate = 5000)
    public void sendTestMessage() {
        try {
            Map<String, Object> testMessage = new HashMap<>();
            testMessage.put("type", "test");
            testMessage.put("message", "서버에서 보내는 테스트 메시지");
            testMessage.put("timestamp", System.currentTimeMillis());
            testMessage.put("kisConnected", kisWebSocketClient.isConnected());
            testMessage.put("subscribedStocks", kisWebSocketClient.getSubscribedStocks());
            
            messagingTemplate.convertAndSend("/topic/test", testMessage);
            log.info("📤 테스트 메시지 브로드캐스트 완료 - KisWebSocket 연결: {}", kisWebSocketClient.isConnected());
            
        } catch (Exception e) {
            log.error("테스트 메시지 전송 실패: {}", e.getMessage());
        }
    }
}

