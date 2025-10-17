package com.example.api_gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "인증 관리", description = "회원가입, 로그인, 토큰 갱신, 로그아웃 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class AuthProxyController {

    private final WebClient userServiceClient;

    public AuthProxyController() {
        this.userServiceClient = WebClient.builder()
                .baseUrl("http://localhost:8082")
                .build();
    }

    // ==================== 인증 관리 ====================
    
    @Operation(summary = "회원가입", description = "새로운 사용자 계정을 생성하고 액세스 토큰과 리프레시 토큰을 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "회원가입 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 중복된 사용자명"),
        @ApiResponse(responseCode = "401", description = "디바이스 ID 누락")
    })
    @PostMapping("/signup")
    public Mono<ResponseEntity<Object>> signup(
            @Parameter(description = "회원가입 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "디바이스 ID", required = true) @RequestHeader("X-Device-Id") String deviceId) {
        
        log.info("회원가입 요청: deviceId={}", deviceId);
        
        return userServiceClient.post()
                .uri("/auth/signup")
                .header("X-Device-Id", deviceId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("회원가입 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "로그인", description = "사용자 인증 후 액세스 토큰과 리프레시 토큰을 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패 또는 디바이스 ID 누락")
    })
    @PostMapping("/login")
    public Mono<ResponseEntity<Object>> login(
            @Parameter(description = "로그인 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "디바이스 ID", required = true) @RequestHeader("X-Device-Id") String deviceId) {
        
        log.info("로그인 요청: deviceId={}", deviceId);
        
        return userServiceClient.post()
                .uri("/auth/login")
                .header("X-Device-Id", deviceId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("로그인 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰과 리프레시 토큰을 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰 또는 디바이스 ID 누락")
    })
    @PostMapping("/refresh")
    public Mono<ResponseEntity<Object>> refresh(
            @Parameter(description = "리프레시 토큰", required = true) @RequestHeader("X-Refresh-Token") String refreshToken,
            @Parameter(description = "디바이스 ID", required = true) @RequestHeader("X-Device-Id") String deviceId) {
        
        log.info("토큰 갱신 요청: deviceId={}", deviceId);
        
        return userServiceClient.post()
                .uri("/auth/refresh")
                .header("X-Refresh-Token", refreshToken)
                .header("X-Device-Id", deviceId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("토큰 갱신 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "로그아웃", description = "사용자의 리프레시 토큰을 무효화하여 로그아웃을 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패 또는 디바이스 ID 누락")
    })
    @PostMapping("/logout")
    public Mono<ResponseEntity<Object>> logout(
            @Parameter(description = "디바이스 ID", required = true) @RequestHeader("X-Device-Id") String deviceId,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("로그아웃 요청: userId={}, deviceId={}", userId, deviceId);
        
        return userServiceClient.post()
                .uri("/auth/logout")
                .header("X-Device-Id", deviceId)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("로그아웃 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}