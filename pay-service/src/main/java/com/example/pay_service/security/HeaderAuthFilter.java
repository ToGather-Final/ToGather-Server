package com.example.pay_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try{
                UUID userId = UUID.fromString(userIdHeader);

                UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("인증 성공 - X-User-Id 헤더: {}", userId);
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 X-User-Id 헤더: {}", userIdHeader);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        } else {
            log.warn("인증 실패 - X-User-Id 헤더 없음");
        }

        chain.doFilter(request, response);
    }
}
