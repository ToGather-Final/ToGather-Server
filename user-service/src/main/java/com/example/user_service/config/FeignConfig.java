package com.example.user_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UUID) {
                    UUID userId = (UUID) authentication.getPrincipal();
                    template.header("X-User-Id", userId.toString());
                    log.debug("Feign 요청에 X-User-Id 헤더 추가: {}", userId);
                } else {
                    log.warn("Feign 요청에 X-User-Id 헤더를 추가할 수 없습니다. 인증 정보 없음 또는 타입 불일치.");
                }
            }
        };
    }
}
