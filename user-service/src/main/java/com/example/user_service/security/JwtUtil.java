package com.example.user_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer:togather}")
    private String issuer;

    @Value("${jwt.access-exp-seconds:1800}")
    private long accessTokenExpireSeconds;

    public String issue(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        Date issuedAt = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        Date expiration = Date.from(now.plusSeconds(accessTokenExpireSeconds).atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuer(issuer)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

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