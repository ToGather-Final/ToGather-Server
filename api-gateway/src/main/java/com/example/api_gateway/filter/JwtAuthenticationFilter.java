package com.example.api_gateway.filter;

import com.example.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        // "Authorization" 헤더가 없거나 "Bearer "로 시작하지 않으면 통과
        if (!headers.containsKey(HttpHeaders.AUTHORIZATION) || !headers.getFirst(HttpHeaders.AUTHORIZATION).startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
        String jwtToken = authorizationHeader.substring(7);

        try {
            if (jwtUtil.validateToken(jwtToken)) {
                Claims claims = jwtUtil.extractAllClaims(jwtToken);
                String userId = claims.get("userId", String.class);
                String username = claims.getSubject();

                // 요청 헤더에 사용자 정보를 추가하여 내부 서비스로 전달
                ServerHttpRequest newRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-Username", username)
                        .build();

                ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
                return chain.filter(newExchange);
            }
        } catch (Exception e) {
            // 토큰 검증 실패 시
            return handleUnauthorized(exchange);
        }

        return handleUnauthorized(exchange);
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        // 필터의 실행 순서, 낮은 숫자가 먼저 실행됨
        return -1;
    }
}

//package com.example.api_gateway.filter;
//
//import com.example.api_gateway.util.JwtUtil;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//
//@Component
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//    @Autowired
//    private JwtUtil jwtUtil;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
//                                    FilterChain filterChain) throws ServletException, IOException {
//
//        final String requestTokenHeader = request.getHeader("Authorization");
//
//        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
//            String jwtToken = requestTokenHeader.substring(7);
//
//            try {
//                if (jwtUtil.validateToken(jwtToken)) {
//                    Map<String, Object> userInfo = jwtUtil.extractUserInfo(jwtToken);
//
//                    // JWT에서 추출한 사용자 정보를 Security Context에 설정
//                    UsernamePasswordAuthenticationToken authToken =
//                        new UsernamePasswordAuthenticationToken(
//                            userInfo.get("username"),
//                            null,
//                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
//                        );
//
//                    // 사용자 정보를 request attribute에 저장 (다른 서비스로 전달용)
//                    request.setAttribute("userId", userInfo.get("userId"));
//                    request.setAttribute("username", userInfo.get("username"));
//                    request.setAttribute("email", userInfo.get("email"));
//                    request.setAttribute("roles", userInfo.get("roles"));
//
//                    SecurityContextHolder.getContext().setAuthentication(authToken);
//                }
//            } catch (Exception e) {
//                logger.error("JWT 토큰 검증 실패: " + e.getMessage());
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }
//}
