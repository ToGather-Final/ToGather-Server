package com.example.api_gateway.filter;

import com.example.api_gateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * API Gateway의 JWT 인증 필터
 * - JWT 토큰을 검증하고 X-User-Id 헤더를 추가하여 하위 서비스로 전달
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // 인증이 필요 없는 경로들
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/refresh",
            "/api/trading/stocks"  // 주식 조회는 인증 불필요
    );

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        // 인증이 필요 없는 경로는 그냥 통과
        if (isExcludedPath(path)) {
            log.debug("인증 제외 경로: {}", path);
            return chain.filter(exchange);
        }

        // Authorization 헤더에서 JWT 토큰 추출
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("JWT 토큰이 없습니다. 경로: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            // JWT 검증 및 사용자 ID 추출
            UUID userId = jwtUtil.verifyAndGetUserId(token);
            log.info("JWT 검증 성공 - UserId: {}, Path: {}", userId, path);

            // X-User-Id 헤더 추가하여 하위 서비스로 전달
            ServerHttpRequest modifiedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", userId.toString())
                    .build();

            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            return chain.filter(modifiedExchange);

        } catch (Exception e) {
            log.error("JWT 검증 실패: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 인증 제외 경로 확인
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -100; // 다른 필터보다 먼저 실행
    }
}

