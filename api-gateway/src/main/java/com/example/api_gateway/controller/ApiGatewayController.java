package com.example.api_gateway.controller;

import com.example.api_gateway.service.MicroserviceProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiGatewayController {

    @Autowired
    private MicroserviceProxyService proxyService;

    @GetMapping("/users/**")
    public ResponseEntity<Object> proxyToUserService(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "user-service", "/api/users");
    }

    @PostMapping("/users/**")
    public ResponseEntity<Object> proxyToUserServicePost(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "user-service", "/api/users");
    }

    @PutMapping("/users/**")
    public ResponseEntity<Object> proxyToUserServicePut(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "user-service", "/api/users");
    }

    @DeleteMapping("/users/**")
    public ResponseEntity<Object> proxyToUserServiceDelete(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "user-service", "/api/users");
    }

    @GetMapping("/trading/**")
    public ResponseEntity<Object> proxyToTradingService(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "trading-service", "/api/trading");
    }

    @PostMapping("/trading/**")
    public ResponseEntity<Object> proxyToTradingServicePost(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "trading-service", "/api/trading");
    }

    @PutMapping("/trading/**")
    public ResponseEntity<Object> proxyToTradingServicePut(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "trading-service", "/api/trading");
    }

    @DeleteMapping("/trading/**")
    public ResponseEntity<Object> proxyToTradingServiceDelete(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "trading-service", "/api/trading");
    }

    @GetMapping("/pay/**")
    public ResponseEntity<Object> proxyToPayService(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "pay-service", "/api/pay");
    }

    @PostMapping("/pay/**")
    public ResponseEntity<Object> proxyToPayServicePost(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "pay-service", "/api/pay");
    }

    @PutMapping("/pay/**")
    public ResponseEntity<Object> proxyToPayServicePut(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "pay-service", "/api/pay");
    }

    @DeleteMapping("/pay/**")
    public ResponseEntity<Object> proxyToPayServiceDelete(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "pay-service", "/api/pay");
    }

    @GetMapping("/vote/**")
    public ResponseEntity<Object> proxyToVoteService(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "vote-service", "/api/vote");
    }

    @PostMapping("/vote/**")
    public ResponseEntity<Object> proxyToVoteServicePost(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "vote-service", "/api/vote");
    }

    @PutMapping("/vote/**")
    public ResponseEntity<Object> proxyToVoteServicePut(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "vote-service", "/api/vote");
    }

    @DeleteMapping("/vote/**")
    public ResponseEntity<Object> proxyToVoteServiceDelete(HttpServletRequest request) {
        return proxyService.proxyRequest(request, "vote-service", "/api/vote");
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "api-gateway"));
    }
}
