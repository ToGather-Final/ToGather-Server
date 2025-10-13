package com.example.trading_service.controller;

import com.example.trading_service.service.KisTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/kis")
@RequiredArgsConstructor
public class KisTokenController {

    private final KisTokenService kisTokenService;

    /**
     * KIS 토큰 상태 조회
     */
    @GetMapping("/token/status")
    public ResponseEntity<Map<String, Object>> getTokenStatus() {
        Map<String, Object> status = kisTokenService.getTokenStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * KIS 토큰 수동 갱신
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<Map<String, String>> refreshToken() {
        try {
            String newToken = kisTokenService.refreshAccessToken();
            return ResponseEntity.ok(Map.of(
                "message", "토큰이 성공적으로 갱신되었습니다.",
                "tokenPreview", newToken.substring(0, 20) + "..."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "토큰 갱신 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * KIS 토큰 캐시 무효화
     */
    @DeleteMapping("/token/cache")
    public ResponseEntity<Map<String, String>> invalidateToken() {
        kisTokenService.invalidateToken();
        return ResponseEntity.ok(Map.of(
            "message", "토큰 캐시가 무효화되었습니다."
        ));
    }
}


