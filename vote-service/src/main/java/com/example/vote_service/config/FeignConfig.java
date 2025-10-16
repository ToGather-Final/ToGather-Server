package com.example.vote_service.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 클라이언트 설정
 * - 로깅 및 인터셉터 설정
 */
@Slf4j
@Configuration
public class FeignConfig {

    /**
     * Feign 로깅 레벨 설정
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 요청 인터셉터 - X-User-Id 헤더 추가
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                log.info("🔍 Feign 요청 인터셉터 - URL: {}, Method: {}", template.url(), template.method());
                log.info("🔍 Feign 요청 헤더: {}", template.headers());
                
                // SecurityContext에서 현재 사용자 ID 가져오기
                try {
                    org.springframework.security.core.context.SecurityContext context = 
                        org.springframework.security.core.context.SecurityContextHolder.getContext();
                    
                    if (context != null && context.getAuthentication() != null) {
                        String userId = context.getAuthentication().getName();
                        if (userId != null && !userId.equals("anonymousUser")) {
                            template.header("X-User-Id", userId);
                            log.info("✅ X-User-Id 헤더 추가: {}", userId);
                        } else {
                            log.warn("⚠️ 인증된 사용자 ID를 찾을 수 없습니다.");
                        }
                    } else {
                        log.warn("⚠️ SecurityContext 또는 Authentication이 null입니다.");
                    }
                } catch (Exception e) {
                    log.error("❌ X-User-Id 헤더 추가 실패: {}", e.getMessage());
                }
            }
        };
    }
}
