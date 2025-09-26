package com.example.api_gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
public class MicroserviceProxyService {

    @Value("${USER_SERVICE_URL:http://user-service:8080}")
    private String userServiceUrl;

    @Value("${TRADING_SERVICE_URL:http://trading-service:8080}")
    private String tradingServiceUrl;

    @Value("${PAY_SERVICE_URL:http://pay-service:8080}")
    private String payServiceUrl;

    @Value("${VOTE_SERVICE_URL:http://vote-service:8080}")
    private String voteServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<Object> proxyRequest(HttpServletRequest request, String serviceName, String basePath) {
        try {
            String targetUrl = getServiceUrl(serviceName);
            String requestPath = request.getRequestURI();
            String queryString = request.getQueryString();
            
            // API Gateway의 /api 경로를 제거하고 실제 서비스 경로로 변환
            String servicePath = requestPath.replaceFirst("/api" + basePath, basePath);
            if (queryString != null) {
                servicePath += "?" + queryString;
            }
            
            String fullUrl = targetUrl + servicePath;

            // 헤더 복사 및 JWT 사용자 정보 추가
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (!headerName.equalsIgnoreCase("host")) {
                    headers.add(headerName, request.getHeader(headerName));
                }
            }

            // JWT에서 추출한 사용자 정보를 헤더에 추가
            Object userId = request.getAttribute("userId");
            Object username = request.getAttribute("username");
            Object email = request.getAttribute("email");
            Object roles = request.getAttribute("roles");

            if (userId != null) {
                headers.add("X-User-Id", userId.toString());
            }
            if (username != null) {
                headers.add("X-Username", username.toString());
            }
            if (email != null) {
                headers.add("X-Email", email.toString());
            }
            if (roles != null) {
                headers.add("X-Roles", roles.toString());
            }

            // HTTP 메서드에 따른 요청 처리
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            HttpEntity<Object> entity = new HttpEntity<>(null, headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                fullUrl, 
                method, 
                entity, 
                Object.class
            );

            return response;

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "서비스 호출 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private String getServiceUrl(String serviceName) {
        return switch (serviceName) {
            case "user-service" -> userServiceUrl;
            case "trading-service" -> tradingServiceUrl;
            case "pay-service" -> payServiceUrl;
            case "vote-service" -> voteServiceUrl;
            default -> throw new IllegalArgumentException("알 수 없는 서비스: " + serviceName);
        };
    }
}
