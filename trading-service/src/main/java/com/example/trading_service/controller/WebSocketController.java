package com.example.trading_service.controller;

import com.example.trading_service.service.KisWebSocketClient;
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

    /**
     * 호가 구독 요청
     */
    @MessageMapping("/orderbook/subscribe")
    @SendTo("/topic/orderbook/status")
    public Map<String, Object> subscribeOrderBook(String stockCode, SimpMessageHeaderAccessor headerAccessor) {
        log.info("호가 구독 요청: {}", stockCode);
        
        Map<String, Object> response = new HashMap<>();
        response.put("action", "subscribe");
        response.put("stockCode", stockCode);
        response.put("status", "success");
        response.put("message", "호가 구독이 시작되었습니다");
        
        return response;
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

