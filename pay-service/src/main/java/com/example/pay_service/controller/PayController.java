package com.example.pay_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pay")
public class PayController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPayments() {
        return ResponseEntity.ok(Map.of(
            "message", "Get all payments",
            "data", "[]",
            "status", "success"
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
            "message", "Get payment by id: " + id,
            "data", Map.of("id", id, "amount", 1000),
            "status", "success"
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody Map<String, Object> paymentData) {
        return ResponseEntity.ok(Map.of(
            "message", "Payment created successfully",
            "data", paymentData,
            "status", "success"
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "pay-service",
            "message", "Pay Service is running"
        ));
    }
}
