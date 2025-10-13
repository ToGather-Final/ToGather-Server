package com.example.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        String password = System.getenv().getOrDefault("REDIS_PASSWORD", "");
        configuration.setHostName(host);
        configuration.setPort(port);
        if (!password.isBlank()) {
            configuration.setPassword(password);
        }
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
//package com.example.user_service.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.data.redis.core.StringRedisTemplate;
//
//@Configuration
//public class RedisConfig {
//
//    @Bean
//    public RedisConnectionFactory redisConnectionFactory() {
//        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
//
//        // Redis URL 파싱 (tcp://host:port 형태 처리)
//        String redisUrl = System.getenv().getOrDefault("REDIS_URL", "");
//        String host = "localhost";
//        int port = 6379;
//        String password = System.getenv().getOrDefault("REDIS_PASSWORD", "");
//
//        if (!redisUrl.isEmpty()) {
//            // tcp://host:port 형태에서 host와 port 추출
//            if (redisUrl.startsWith("tcp://")) {
//                String[] parts = redisUrl.substring(6).split(":");
//                if (parts.length >= 2) {
//                    host = parts[0];
//                    port = Integer.parseInt(parts[1]);
//                }
//            }
//        } else {
//            // 개별 환경 변수 사용
//            host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
//            String portStr = System.getenv().getOrDefault("REDIS_PORT", "6379");
//            // REDIS_PORT가 URL 형태인 경우 처리
//            if (portStr.contains("tcp://")) {
//                String[] parts = portStr.substring(6).split(":");
//                if (parts.length >= 2) {
//                    host = parts[0];
//                    port = Integer.parseInt(parts[1]);
//                }
//            } else {
//                port = Integer.parseInt(portStr);
//            }
//        }
//
//        configuration.setHostName(host);
//        configuration.setPort(port);
//        if (!password.isBlank()) {
//            configuration.setPassword(password);
//        }
//        return new LettuceConnectionFactory(configuration);
//    }
//
//    @Bean
//    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
//        return new StringRedisTemplate(factory);
//    }
//}
