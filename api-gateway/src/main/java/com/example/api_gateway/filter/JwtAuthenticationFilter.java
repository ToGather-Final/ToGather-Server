package com.example.api_gateway.filter;

import com.example.api_gateway.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        final String requestTokenHeader = request.getHeader("Authorization");
        
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            String jwtToken = requestTokenHeader.substring(7);
            
            try {
                if (jwtUtil.validateToken(jwtToken)) {
                    Map<String, Object> userInfo = jwtUtil.extractUserInfo(jwtToken);
                    
                    // JWT에서 추출한 사용자 정보를 Security Context에 설정
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userInfo.get("username"), 
                            null, 
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                    
                    // 사용자 정보를 request attribute에 저장 (다른 서비스로 전달용)
                    request.setAttribute("userId", userInfo.get("userId"));
                    request.setAttribute("username", userInfo.get("username"));
                    request.setAttribute("email", userInfo.get("email"));
                    request.setAttribute("roles", userInfo.get("roles"));
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                logger.error("JWT 토큰 검증 실패: " + e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
