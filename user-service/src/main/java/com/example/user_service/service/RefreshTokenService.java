package com.example.user_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final long refreshExpireDays;

    public RefreshTokenService(StringRedisTemplate redisTemplate,
                               @Value("${jwt.refresh-exp-days:7}") long refreshExpireDays) {
        this.redisTemplate = redisTemplate;
        this.refreshExpireDays = refreshExpireDays;
    }

    public String issue(UUID userId, String deviceId) {
        validateDeviceId(deviceId);
        String refreshToken = newTokenString();
        String key = buildKey(userId, deviceId);
        String value = sha256(refreshToken);
        Duration ttl = Duration.ofDays(refreshExpireDays);
        redisTemplate.opsForValue().set(key, value, ttl);
        return refreshToken;
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
