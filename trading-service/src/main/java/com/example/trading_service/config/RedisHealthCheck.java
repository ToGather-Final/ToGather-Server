package com.example.trading_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
public class RedisHealthCheck {

    private final StringRedisTemplate redisTemplate;

    @PostConstruct
    public void checkRedisHealth() {
        System.out.println("🔄 Redis 연결 상태 확인 시작...");
        try {
            redisTemplate.opsForValue().set("redis:health_check", "ok");
            String value = redisTemplate.opsForValue().get("redis:health_check");
            System.out.println("✅ Redis 연결 성공: " + value);
        } catch (Exception e) {
            System.err.println("❌ Redis 연결 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}



