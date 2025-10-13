package com.example.vote_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.UUID;

/**
 * JWT 토큰 처리 유틸리티
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * JWT 시크릿 키 생성
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    public UUID getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String userIdStr = claims.getSubject();
            return UUID.fromString(userIdStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다.", e);
        }
    }

    /**
     * JWT 토큰 유효성 검증 - 위조됐는지 확인
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * JWT 토큰 검증 후 사용자 ID 반환
     * - 토큰이 유효하면 userId 반환
     * - 토큰이 유효하지 않으면 예외 발생
     */
    public UUID verifyAndGetUserId(String token) {
        if (!validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다.");
        }
        return getUserIdFromToken(token);
    }
}