package com.example.user_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.issuer:togather}")
    private String issuer;

    @Value("${app.jwt.access-exp-seconds:1800}")
    private long accessTokenExpireSeconds;

//    public String issue(UUID userId) {
//        LocalDateTime now = LocalDateTime.now();
//        Date issuedAt = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
//        Date expiration = Date.from(now.plusSeconds(accessTokenExpireSeconds).atZone(ZoneId.systemDefault()).toInstant());
//
//        return Jwts.builder()
//                .setSubject(userId.toString())
//                .setIssuer(issuer)
//                .setIssuedAt(issuedAt)
//                .setExpiration(expiration)
//                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
//                .compact();
//    }
    public String issue(UUID userId) {
        log.debug("JWT 생성 시작 - userId: {}", userId);

        try {
            LocalDateTime now = LocalDateTime.now();
            Date issuedAt = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
            Date expiration = Date.from(now.plusSeconds(accessTokenExpireSeconds).atZone(ZoneId.systemDefault()).toInstant());

            String token = Jwts.builder()
                    .setSubject(userId.toString())
                    .setIssuer(issuer)
                    .setIssuedAt(issuedAt)
                    .setExpiration(expiration)
                    .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .compact();

            log.debug("JWT 생성 성공");
            return token;
        } catch (Exception e) {
            log.error("JWT 생성 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

