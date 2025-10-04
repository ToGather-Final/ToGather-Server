package com.example.user_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }
        UUID userId = resolveUserIdFromGateway(request);
        if (userId != null) {
            setAuthentication(userId);
            chain.doFilter(request, response);
            return;
        }
        String token = resolveBearerToken(request);
        if (token != null) {
            UUID parsedUserId = parseUserIdFromToken(token);
            if (parsedUserId != null) {
                setAuthentication(parsedUserId);
            }
        }
        chain.doFilter(request, response);
    }

    private UUID resolveUserIdFromGateway(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header == null) {
            return null;
        }
        return parseUuid(header);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null) {
            return null;
        }
        if (!header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }

    private UUID parseUserIdFromToken(String token) {
        try {
            return jwtUtil.verifyAndGetUserId(token);
        } catch (Exception e) {
            return null;
        }
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void setAuthentication(UUID userId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }


}
