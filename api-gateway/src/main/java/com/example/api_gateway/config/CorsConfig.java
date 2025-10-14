package com.example.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * API Gateway CORS 설정 (WebFlux용)
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // 허용할 Origin
        corsConfig.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        
        // 허용할 HTTP 메서드
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 허용할 헤더
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        
        // Credentials 허용
        corsConfig.setAllowCredentials(true);
        
        // preflight 요청 캐시 시간 (초)
        corsConfig.setMaxAge(3600L);
        
        // 노출할 헤더
        corsConfig.setExposedHeaders(Arrays.asList("Authorization", "X-User-Id"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}

