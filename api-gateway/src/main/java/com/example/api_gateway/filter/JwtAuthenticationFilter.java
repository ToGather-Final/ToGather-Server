package com.example.api_gateway.filter;

import com.example.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
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
            "/api/trading/stocks",  // 주식 조회는 인증 불필요
            "/api/websocket",       // WebSocket API는 인증 불필요
            "/ws",                  // WebSocket 연결 엔드포인트
            "/actuator"             // Spring Actuator 엔드포인트
    );

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        log.info("=== JWT 필터 초기화 ===");
        log.info("JWT_SECRET_KEY 환경변수: {}", System.getenv("JWT_SECRET_KEY") != null ? "설정됨" : "NULL");
        log.info("JwtUtil 주입됨: {}", jwtUtil != null ? "성공" : "실패");
        log.info("=====================");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        
        log.info("🔍 JWT 필터 실행됨 - 경로: {}", path);
        log.info("=== API Gateway 요청 수신 ===");
        log.info("경로: {}", path);
        log.info("메서드: {}", exchange.getRequest().getMethod());
        log.info("요청 URI: {}", exchange.getRequest().getURI());
        log.info("모든 헤더: {}", exchange.getRequest().getHeaders());
        log.info("==============================");
        
        // 인증이 필요 없는 경로는 그냥 통과
        if (isExcludedPath(path)) {
            log.info("인증 제외 경로로 통과: {}", path);
            return chain.filter(exchange)
                    .doOnSuccess(result -> log.info("라우팅 성공: {}", path))
                    .doOnError(error -> log.error("라우팅 실패: {} - {}", path, error.getMessage()));
        }

        // Authorization 헤더에서 JWT 토큰 추출
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.info("Authorization 헤더: {}", authHeader);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("JWT 토큰이 없습니다. 경로: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        log.info("추출된 JWT 토큰: {}", token.substring(0, Math.min(20, token.length())) + "...");

        try {
            // JWT 검증 및 사용자 ID 추출
            UUID userId = jwtUtil.verifyAndGetUserId(token);
            log.info("JWT 검증 성공 - UserId: {}, Path: {}", userId, path);

            // X-User-Id 헤더 추가하여 하위 서비스로 전달
            ServerHttpRequest modifiedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", userId.toString())
                    .build();

            log.info("X-User-Id 헤더 추가: {} -> {}", path, userId);

            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(modifiedRequest)
                    .build();

            return chain.filter(modifiedExchange)
                    .doOnSuccess(result -> log.info("인증된 요청 라우팅 성공: {} (UserId: {})", path, userId))
                    .doOnError(error -> log.error("인증된 요청 라우팅 실패: {} (UserId: {}) - {}", path, userId, error.getMessage()));

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
        // WebSocket 경로는 특별 처리
        if (path.startsWith("/ws")) {
            return true;
        }
        
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -100; // 다른 필터보다 먼저 실행
    }
}
