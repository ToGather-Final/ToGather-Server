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
 * 1. 클라이언트가 API 호출 → 헤더에 JWT 포함
 * 2. JwtAuthFilter가 토큰 확인 → 유효하면 인증 성공
 * 3. 인증 실패면 RestAuthEntryPoint가 401 리턴
 * 4. 인증 성공해도 권한이 없으면 RestAccessDeniedHandler가 403 리턴
 * 5. 나머지는 컨트롤러 로직 실행
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
        http.cors(cors -> {});
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(ex -> {
            ex.authenticationEntryPoint(restAuthEntryPoint);
            ex.accessDeniedHandler(restAccessDeniedHandler);
        });
        http.authorizeHttpRequests(reg -> {
            reg.anyRequest().authenticated();
        });
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

