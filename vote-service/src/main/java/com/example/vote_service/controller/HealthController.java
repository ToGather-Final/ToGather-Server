package com.example.vote_service.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@Hidden
@Tag(name = "헬스 체크", description = "Vote Service 상태 확인 관련 API")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Hidden
    @Operation(summary = "ALB 헬스 체크", description = "ALB에서 사용하는 단순 OK 응답")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vote Service 정상 동작")
    })
    @GetMapping("/api/healthz")
    public ResponseEntity<String> healthz() {
        return ResponseEntity.ok("OK");
    }

    @Hidden
    @Operation(summary = "Liveness Probe", description = "JVM 및 스레드 상태 확인")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "서비스 정상 동작"),
        @ApiResponse(responseCode = "503", description = "서비스 비정상")
    })
    @GetMapping("/actuator/health/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
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
                return ResponseEntity.status(503).body(health);
            }
            
            health.put("status", "UP");
            health.put("memory", Map.of(
                "usage_percent", String.format("%.2f%%", memoryUsagePercent),
                "used_mb", usedMemory / 1024 / 1024,
                "max_mb", maxMemory / 1024 / 1024
            ));
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    @Hidden
    @Operation(summary = "Readiness Probe", description = "데이터베이스 및 Redis 연결 상태 확인")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "서비스 준비 완료"),
        @ApiResponse(responseCode = "503", description = "서비스 준비 미완료")
    })
    @GetMapping("/actuator/health/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> checks = new HashMap<>();
        boolean allHealthy = true;
        
        // 데이터베이스 연결 확인
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                checks.put("database", Map.of("status", "UP"));
            } else {
                checks.put("database", Map.of("status", "DOWN", "error", "Connection invalid"));
                allHealthy = false;
            }
        } catch (Exception e) {
            checks.put("database", Map.of("status", "DOWN", "error", e.getMessage()));
            allHealthy = false;
        }
        
        // Redis 연결 확인
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            if ("PONG".equals(pong)) {
                checks.put("redis", Map.of("status", "UP"));
            } else {
                checks.put("redis", Map.of("status", "DOWN", "error", "Unexpected ping response"));
                allHealthy = false;
            }
        } catch (Exception e) {
            checks.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
            allHealthy = false;
        }
        
        health.put("status", allHealthy ? "UP" : "DOWN");
        health.put("checks", checks);
        
        return allHealthy ? ResponseEntity.ok(health) : ResponseEntity.status(503).body(health);
    }
}
