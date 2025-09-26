package com.example.trading_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trading")
public class TradingController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTradings() {
        return ResponseEntity.ok(Map.of(
            "message", "Get all tradings",
            "data", "[]",
            "status", "success"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTradingById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
            "message", "Get trading by id: " + id,
            "data", Map.of("id", id, "item", "Test Item"),
            "status", "success"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTrading(@RequestBody Map<String, Object> tradingData) {
        return ResponseEntity.ok(Map.of(
            "message", "Trading created successfully",
            "data", tradingData,
            "status", "success"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "trading-service",
            "message", "Trading Service is running"
        ));
    }
}
