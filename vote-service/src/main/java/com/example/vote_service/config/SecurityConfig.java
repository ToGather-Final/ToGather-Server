package com.example.vote_service.config;

import com.example.vote_service.error.RestAccessDeniedHandler;
import com.example.vote_service.error.RestAuthEntryPoint;
import com.example.vote_service.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 보안 설정
 * API Gateway에서 JWT 검증을 수행하고 X-User-Id 헤더를 전달받음
 * JwtAuthFilter는 X-User-Id 헤더만 읽어서 인증 처리
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthEntryPoint restAuthEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RestAuthEntryPoint restAuthEntryPoint,
                          RestAccessDeniedHandler restAccessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.restAuthEntryPoint = restAuthEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        // CORS는 API Gateway에서 처리
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(ex -> {
            ex.authenticationEntryPoint(restAuthEntryPoint);
            ex.accessDeniedHandler(restAccessDeniedHandler);
        });
        http.authorizeHttpRequests(reg -> {
            reg.requestMatchers("/actuator/**").permitAll();
            reg.requestMatchers("/v3/api-docs/**").permitAll();
            reg.requestMatchers("/swagger-ui/**").permitAll();
            reg.requestMatchers("/redoc").permitAll();
            reg.requestMatchers("/redoc-standalone").permitAll();
            reg.requestMatchers("/internal/**").permitAll(); // 내부 API 엔드포인트 인증 우회
            reg.anyRequest().authenticated();
        });
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

