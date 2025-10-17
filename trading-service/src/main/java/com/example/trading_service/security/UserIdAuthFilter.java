package com.example.trading_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * API Gateway에서 검증된 X-User-Id 헤더를 읽어 인증 처리
 * JWT 검증은 API Gateway에서 수행됨
 */
@Slf4j
@Component
public class UserIdAuthFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 인증이 필요 없는 경로는 필터를 건너뜁니다
        return path.startsWith("/trading/stocks") ||  // 주식 조회
               path.startsWith("/api/websocket") ||   // WebSocket API
               path.startsWith("/ws") ||              // WebSocket 연결
               path.equals("/status") ||              // RewritePath로 변환된 경로
               path.equals("/reconnect") ||
               path.equals("/test-approval-key") ||
               path.equals("/cache-status") ||        // 캐시 상태 확인
               path.startsWith("/cached-orderbook") || // 캐시된 호가 데이터 조회
               path.startsWith("/swagger-ui") || 
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        // API Gateway에서 전달한 X-User-Id 헤더 읽기
        log.info("=== Trading Service 요청 수신 ===");
        log.info("경로: {}", request.getRequestURI());
        log.info("메서드: {}", request.getMethod());
        
        UUID userId = resolveUserIdFromHeader(request);
        if (userId != null) {
            log.info("인증 성공 - X-User-Id 헤더: {}", userId);
            setAuthentication(userId);
        } else {
            log.warn("X-User-Id 헤더가 없습니다. 경로: {}", request.getRequestURI());
        }
        
        chain.doFilter(request, response);
    }

    private UUID resolveUserIdFromHeader(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 X-User-Id 형식: {}", userIdHeader);
            return null;
        }
    }

    private void setAuthentication(UUID userId) {
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(userId,
                null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

