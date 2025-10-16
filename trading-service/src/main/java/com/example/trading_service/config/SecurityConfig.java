package com.example.trading_service.config;

import com.example.trading_service.security.UserIdAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 보안 설정
 * API Gateway에서 JWT 검증을 수행하고 X-User-Id 헤더를 전달받음
 * UserIdAuthFilter는 X-User-Id 헤더만 읽어서 인증 처리
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserIdAuthFilter userIdAuthFilter;

    public SecurityConfig(UserIdAuthFilter userIdAuthFilter) {
        this.userIdAuthFilter = userIdAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // CORS는 API Gateway에서 처리
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 주식 조회는 인증 불필요
                        .requestMatchers("/trading/stocks", "/trading/stocks/**").permitAll()
                        // Swagger UI 및 API 문서는 인증 불필요
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/index.html", "/swagger-resources/**", "/webjars/**").permitAll()
                        // favicon.ico 허용
                        .requestMatchers("/favicon.ico").permitAll()
                        // 나머지는 인증 필요
                        // Actuator 엔드포인트는 인증 불필요 (Health Check용)
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(userIdAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}


