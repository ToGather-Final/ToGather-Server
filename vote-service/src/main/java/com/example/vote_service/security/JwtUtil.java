package com.example.vote_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 유틸리티 클래스
 * - vote-service는 JWT 토큰을 발행하지 않고 검증만 수행
 * - JWT 발행은 user-service에서만 수행
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * JWT 토큰 검증 및 사용자 ID 추출
     * @param token JWT 토큰 문자열
     * @return 토큰에서 추출한 사용자 ID
     * @throws io.jsonwebtoken.JwtException 토큰이 유효하지 않은 경우
     */
    public UUID verifyAndGetUserId(String token) {
        String subject = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        return UUID.fromString(subject);
    }
}

