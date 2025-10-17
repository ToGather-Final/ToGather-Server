package com.example.api_gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "헬스 체크", description = "API Gateway 상태 확인 관련 API")
public class HealthController {

    @Operation(summary = "API Gateway 헬스 체크", description = "API Gateway의 기본 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "API Gateway 정상 동작")
    })
    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("OK"));
    }

    @Operation(summary = "API 헬스 체크", description = "API 엔드포인트의 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "API 엔드포인트 정상 동작")
    })
    @GetMapping("/api/health")
    public Mono<ResponseEntity<String>> apiHealth() {
        return Mono.just(ResponseEntity.ok("OK"));
    }
}
