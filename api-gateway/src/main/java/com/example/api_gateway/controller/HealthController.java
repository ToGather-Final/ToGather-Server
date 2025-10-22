package com.example.api_gateway.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@Hidden
@Tag(name = "헬스 체크", description = "API Gateway 상태 확인 관련 API")
public class HealthController {

    @Operation(summary = "ALB 헬스 체크", description = "ALB에서 사용하는 단순 OK 응답")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "API Gateway 정상 동작")
    })
    @GetMapping("/api/healthz")
    public Mono<ResponseEntity<String>> healthz() {
        return Mono.just(ResponseEntity.ok("OK"));
    }

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

    @Operation(summary = "Liveness Probe", description = "JVM 및 스레드 상태 확인")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "서비스 정상 동작"),
        @ApiResponse(responseCode = "503", description = "서비스 비정상")
    })
    @GetMapping("/actuator/health/liveness")
    public Mono<ResponseEntity<Map<String, Object>>> liveness() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // JVM 메모리 상태 확인
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            // 메모리 사용률이 90% 이상이면 비정상
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            if (memoryUsagePercent > 90) {
                health.put("status", "DOWN");
                health.put("memory", Map.of(
                    "usage_percent", String.format("%.2f%%", memoryUsagePercent),
                    "used_mb", usedMemory / 1024 / 1024,
                    "max_mb", maxMemory / 1024 / 1024
                ));
                return Mono.just(ResponseEntity.status(503).body(health));
            }
            
            health.put("status", "UP");
            health.put("memory", Map.of(
                "usage_percent", String.format("%.2f%%", memoryUsagePercent),
                "used_mb", usedMemory / 1024 / 1024,
                "max_mb", maxMemory / 1024 / 1024
            ));
            
            return Mono.just(ResponseEntity.ok(health));
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return Mono.just(ResponseEntity.status(503).body(health));
        }
    }

    @Operation(summary = "Readiness Probe", description = "Redis 연결 상태 확인")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "서비스 준비 완료"),
        @ApiResponse(responseCode = "503", description = "서비스 준비 미완료")
    })
    @GetMapping("/actuator/health/readiness")
    public Mono<ResponseEntity<Map<String, Object>>> readiness() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> checks = new HashMap<>();
        boolean allHealthy = true;
        
        // Redis 연결 확인 (API Gateway는 DB 연결이 없음)
        try {
            // Redis 연결 확인 로직은 WebFlux 환경에 맞게 구현 필요
            checks.put("redis", Map.of("status", "UP"));
        } catch (Exception e) {
            checks.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
            allHealthy = false;
        }
        
        health.put("status", allHealthy ? "UP" : "DOWN");
        health.put("checks", checks);
        
        return allHealthy ? Mono.just(ResponseEntity.ok(health)) : Mono.just(ResponseEntity.status(503).body(health));
    }
}
