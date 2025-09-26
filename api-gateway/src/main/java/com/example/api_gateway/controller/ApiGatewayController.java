package com.example.api_gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiGatewayController {

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/users/**")
    public ResponseEntity<Object> proxyToUserService(HttpServletRequest request) {
        return proxyRequest(request, "http://user-service:8080");
    }

    @PostMapping("/users/**")
    public ResponseEntity<Object> proxyToUserServicePost(HttpServletRequest request) {
        return proxyRequest(request, "http://user-service:8080");
    }

    @PutMapping("/users/**")
    public ResponseEntity<Object> proxyToUserServicePut(HttpServletRequest request) {
        return proxyRequest(request, "http://user-service:8080");
    }

    @DeleteMapping("/users/**")
    public ResponseEntity<Object> proxyToUserServiceDelete(HttpServletRequest request) {
        return proxyRequest(request, "http://user-service:8080");
    }

    @GetMapping("/trading/**")
    public ResponseEntity<Object> proxyToTradingService(HttpServletRequest request) {
        return proxyRequest(request, "http://trading-service:8080");
    }

    @PostMapping("/trading/**")
    public ResponseEntity<Object> proxyToTradingServicePost(HttpServletRequest request) {
        return proxyRequest(request, "http://trading-service:8080");
    }

    @PutMapping("/trading/**")
    public ResponseEntity<Object> proxyToTradingServicePut(HttpServletRequest request) {
        return proxyRequest(request, "http://trading-service:8080");
    }

    @DeleteMapping("/trading/**")
    public ResponseEntity<Object> proxyToTradingServiceDelete(HttpServletRequest request) {
        return proxyRequest(request, "http://trading-service:8080");
    }

    @GetMapping("/pay/**")
    public ResponseEntity<Object> proxyToPayService(HttpServletRequest request) {
        return proxyRequest(request, "http://pay-service:8080");
    }

    @PostMapping("/pay/**")
    public ResponseEntity<Object> proxyToPayServicePost(HttpServletRequest request) {
        return proxyRequest(request, "http://pay-service:8080");
    }

    @PutMapping("/pay/**")
    public ResponseEntity<Object> proxyToPayServicePut(HttpServletRequest request) {
        return proxyRequest(request, "http://pay-service:8080");
    }

    @DeleteMapping("/pay/**")
    public ResponseEntity<Object> proxyToPayServiceDelete(HttpServletRequest request) {
        return proxyRequest(request, "http://pay-service:8080");
    }

    @GetMapping("/vote/**")
    public ResponseEntity<Object> proxyToVoteService(HttpServletRequest request) {
        return proxyRequest(request, "http://vote-service:8080");
    }

    @PostMapping("/vote/**")
    public ResponseEntity<Object> proxyToVoteServicePost(HttpServletRequest request) {
        return proxyRequest(request, "http://vote-service:8080");
    }

    @PutMapping("/vote/**")
    public ResponseEntity<Object> proxyToVoteServicePut(HttpServletRequest request) {
        return proxyRequest(request, "http://vote-service:8080");
    }

    @DeleteMapping("/vote/**")
    public ResponseEntity<Object> proxyToVoteServiceDelete(HttpServletRequest request) {
        return proxyRequest(request, "http://vote-service:8080");
    }

    private ResponseEntity<Object> proxyRequest(HttpServletRequest request, String targetUrl) {
        try {
            String requestPath = request.getRequestURI();
            String queryString = request.getQueryString();
            
            String fullUrl = targetUrl + requestPath;
            if (queryString != null) {
                fullUrl += "?" + queryString;
            }
            
            // 간단한 프록시 요청
            return restTemplate.getForEntity(fullUrl, Object.class);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Service unavailable: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "api-gateway"));
    }
}
