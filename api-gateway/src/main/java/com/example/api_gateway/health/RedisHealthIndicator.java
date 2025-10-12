package com.example.api_gateway.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            // Redis 연결 테스트
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            
            if ("PONG".equals(pong)) {
                return Health.up()
                        .withDetail("redis", "Available")
                        .withDetail("ping", pong)
                        .build();
            } else {
                return Health.down()
                        .withDetail("redis", "Unavailable")
                        .withDetail("ping", pong)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "Unavailable")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
