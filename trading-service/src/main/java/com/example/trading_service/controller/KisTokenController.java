package com.example.trading_service.controller;

import com.example.trading_service.service.KisTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/kis")
@RequiredArgsConstructor
public class KisTokenController {

    private final KisTokenService kisTokenService;

    @Operation(summary = "KIS 토큰 상태 조회", description = "한국투자증권 API 토큰의 현재 상태를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 상태 조회 성공")
    })
    @GetMapping("/token/status")
    public ResponseEntity<Map<String, Object>> getTokenStatus() {
        Map<String, Object> status = kisTokenService.getTokenStatus();
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "KIS 토큰 수동 갱신", description = "한국투자증권 API 토큰을 수동으로 갱신합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "토큰 갱신 실패")
    })
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

    @Operation(summary = "KIS 토큰 캐시 무효화", description = "한국투자증권 API 토큰 캐시를 무효화합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 캐시 무효화 성공")
    })
    @DeleteMapping("/token/cache")
    public ResponseEntity<Map<String, String>> invalidateToken() {
        kisTokenService.invalidateToken();
        return ResponseEntity.ok(Map.of(
            "message", "토큰 캐시가 무효화되었습니다."
        ));
    }
}


