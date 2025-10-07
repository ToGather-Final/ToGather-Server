package com.example.api_gateway.filter;

import com.example.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        if (!headers.containsKey(HttpHeaders.AUTHORIZATION) || !headers.getFirst(HttpHeaders.AUTHORIZATION).startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        String jwtToken = authorizationHeader.substring(7);

        try {
            if (jwtUtil.validateToken(jwtToken)) {
                Claims claims = jwtUtil.extractAllClaims(jwtToken);
                String userId = claims.get("userId", String.class);
                String username = claims.getSubject();

                ServerHttpRequest newRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-Username", username)
                        .build();

                ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
                return chain.filter(newExchange);
            }
        } catch (Exception e) {
            return handleUnauthorized(exchange);
        }

        return handleUnauthorized(exchange);
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        // 필터의 실행 순서, 낮은 숫자가 먼저 실행됨
        return -1;
    }
}