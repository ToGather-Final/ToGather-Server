package com.example.api_gateway.filter;

import com.example.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        
        log.debug("🔍 JWT Filter - Processing request: {} {}", method, path);
        
        // 인증이 필요없는 경로들 (헬스체크, 공개 API 등)
        if (isPublicPath(path)) {
            log.debug("✅ JWT Filter - Public path, skipping authentication: {}", path);
            return chain.filter(exchange);
        }

        HttpHeaders headers = request.getHeaders();
        String authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("❌ JWT Filter - Authorization header missing or invalid for path: {}", path);
            return handleUnauthorized(exchange, "Authorization header is missing or invalid");
        }

        String jwtToken = authorizationHeader.substring(7);
        log.debug("🔑 JWT Filter - Extracted token: {}...", jwtToken.substring(0, Math.min(20, jwtToken.length())));

        try {
            if (jwtUtil.validateToken(jwtToken)) {
                Claims claims = jwtUtil.extractAllClaims(jwtToken);
                String userId = claims.get("userId", String.class);
                String username = claims.getSubject();
                String email = claims.get("email", String.class);
                String roles = claims.get("roles", String.class);

                log.debug("✅ JWT Filter - Token valid. User: {} (ID: {}), Email: {}, Roles: {}", 
                         username, userId, email, roles);

                // JWT 정보를 헤더에 추가하여 백엔드 서비스로 전달
                ServerHttpRequest newRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId : "")
                        .header("X-Username", username != null ? username : "")
                        .header("X-Email", email != null ? email : "")
                        .header("X-Roles", roles != null ? roles : "")
                        .build();

                ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
                log.debug("🚀 JWT Filter - Forwarding request to backend service");
                return chain.filter(newExchange);
            } else {
                log.warn("❌ JWT Filter - Token validation failed for path: {}", path);
                return handleUnauthorized(exchange, "Invalid JWT token");
            }
        } catch (Exception e) {
            log.error("❌ JWT Filter - JWT validation error for path {}: {}", path, e.getMessage(), e);
            return handleUnauthorized(exchange, "JWT validation failed: " + e.getMessage());
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator/") || 
               path.startsWith("/api/auth/login") || 
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/signup") ||
               path.equals("/api/health") ||
               path.equals("/health");
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        
        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
        );
    }

    @Override
    public int getOrder() {
        // 필터의 실행 순서, 낮은 숫자가 먼저 실행됨
        // Spring Security 필터보다 나중에 실행되도록 1로 설정
        return 1;
    }
}