package com.example.vote_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
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

        UUID userIdFromGateway = resolveUserIdFromHeader(request);
        if (userIdFromGateway != null) {
            setAuthentication(userIdFromGateway);
            chain.doFilter(request, response);
            return;
        }

        UUID userIdFromBearer = resolveUserIdFromBearer(request);
        if (userIdFromBearer != null) {
            setAuthentication(userIdFromBearer);
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
            return null;
        }
    }

    private UUID resolveUserIdFromBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null) {
            return null;
        }
        if (!header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7);
//        try {
//            return jwtUtil.verifyAndGetUserId(token);
//        } catch (Exception e) {
//            return null;
//        }
        return null;
    }

    private void setAuthentication(UUID userId) {
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(userId,
                null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

