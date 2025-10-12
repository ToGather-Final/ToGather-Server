package com.example.user_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final long refreshExpireDays;

    public RefreshTokenService(StringRedisTemplate redisTemplate,
                               @Value("${app.jwt.refresh-exp-days:7}") long refreshExpireDays) {
        this.redisTemplate = redisTemplate;
        this.refreshExpireDays = refreshExpireDays;
    }

//    public String issue(UUID userId, String deviceId) {
//        validateDeviceId(deviceId);
//        String refreshToken = newTokenString();
//        String key = buildKey(userId, deviceId);
//        String value = sha256(refreshToken);
//        Duration ttl = Duration.ofDays(refreshExpireDays);
//        redisTemplate.opsForValue().set(key, value, ttl);
//        return refreshToken;
//    }
    public String issue(UUID userId, String deviceId) {
        System.out.println("=== RefreshToken 생성 시작 ===");
        System.out.println("userId: " + userId);
        System.out.println("deviceId: " + deviceId);
        System.out.println("refreshExpireDays: " + refreshExpireDays);

        try {
            validateDeviceId(deviceId);
            String refreshToken = newTokenString();
            String key = buildKey(userId, deviceId);
            String value = sha256(refreshToken);
            Duration ttl = Duration.ofDays(refreshExpireDays);
            redisTemplate.opsForValue().set(key, value, ttl);

            System.out.println("RefreshToken 생성 성공: " + refreshToken);
            return refreshToken;
        } catch (Exception e) {
            System.out.println("RefreshToken 생성 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public boolean validate(UUID userId, String deviceId, String providedToken) {
        validateDeviceId(deviceId);
        String key = buildKey(userId, deviceId);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return false;
        }
        String providedHash = sha256(providedToken);
        return stored.equals(providedHash);
    }

    public void rotate(UUID userId, String deviceId, String newToken) {
        validateDeviceId(deviceId);
        String key = buildKey(userId, deviceId);
        String value = sha256(newToken);
        Duration ttl = Duration.ofDays(refreshExpireDays);
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public void revoke(UUID userId, String deviceId) {
        validateDeviceId(deviceId);
        String key = buildKey(userId, deviceId);
        redisTemplate.delete(key);
    }

    public UUID getUserIdFromToken(String refreshToken, String deviceId) {
        validateDeviceId(deviceId);

        Set<String> keys = redisTemplate.keys("rt:user:*:" + deviceId);

        for (String key : keys) {
            String storedHash = redisTemplate.opsForValue().get(key);
            if (storedHash != null && storedHash.equals(sha256(refreshToken))) {
                String userIdStr = key.substring(8, key.length() - deviceId.length() - 1);
                return UUID.fromString(userIdStr);
            }
        }

        throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
    }

    private void validateDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId가 필요합니다.");
        }
    }

    private String buildKey(UUID userId, String deviceId) {
        return "rt:user:" + userId + ":" + deviceId;
    }

    private String newTokenString() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        return a.toString() + b.toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("해시 생성 실패");
        }
    }
}
