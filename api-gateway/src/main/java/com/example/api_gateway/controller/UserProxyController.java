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

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Slf4j
@Tag(name = "사용자 관리", description = "사용자 정보 조회, 수정, 중복 확인 관련 API")
@SecurityRequirement(name = "bearerAuth")
public class UserProxyController {

    private final WebClient userServiceClient;

    public UserProxyController() {
        this.userServiceClient = WebClient.builder()
                .baseUrl("http://localhost:8082")
                .build();
    }

    // ==================== 사용자 정보 관리 ====================
    
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/me")
    public Mono<ResponseEntity<Object>> getMe(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("내 정보 조회 요청: userId={}", userId);
        
        return userServiceClient.get()
                .uri("/users/me")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("내 정보 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "닉네임 수정", description = "현재 사용자의 닉네임을 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "닉네임 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PatchMapping("/me/nickname")
    public Mono<ResponseEntity<Object>> updateNickname(
            @Parameter(description = "닉네임 수정 요청 데이터", required = true) @RequestBody Object request,
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        log.info("닉네임 수정 요청: userId={}", userId);
        
        return userServiceClient.patch()
                .uri("/users/me/nickname")
                .header("X-User-Id", userId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("닉네임 수정 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "사용자명 중복 확인", description = "특정 사용자명이 이미 존재하는지 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자명 중복 확인 성공")
    })
    @GetMapping("/exists")
    public Mono<ResponseEntity<Object>> checkUsernameExists(
            @Parameter(description = "확인할 사용자명", required = true) @RequestParam String username) {
        
        log.info("사용자명 중복 확인 요청: username={}", username);
        
        return userServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/exists")
                        .queryParam("username", username)
                        .build())
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("사용자명 중복 확인 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

    @Operation(summary = "사용자 닉네임 조회", description = "특정 사용자의 닉네임을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "닉네임 조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}/nickname")
    public Mono<ResponseEntity<Object>> getUserNickname(
            @Parameter(description = "사용자 ID", required = true) @PathVariable UUID userId) {
        
        log.info("사용자 닉네임 조회 요청: userId={}", userId);
        
        return userServiceClient.get()
                .uri("/users/{userId}/nickname", userId)
                .retrieve()
                .bodyToMono(Object.class)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("사용자 닉네임 조회 실패: {}", e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }
}