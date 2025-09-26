package com.example.user_service.controller;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.dto.TokenResponse;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties.Http;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        validateRegister(request);
        UUID userId = authService.register(request);
        if (userId == null) {
            throw new IllegalArgumentException("등록 실패");
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        validateLogin(request);
        UUID userId = authService.login(request);
        String accessToken = jwtUtil.issue(userId);
        return new TokenResponse(accessToken);
    }

    private void validateRegister(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청이 비었습니다.");
        }
    }

    private void validateLogin(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청이 비었습니다.");
        }
    }
}
