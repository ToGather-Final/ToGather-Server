package com.example.user_service.controller;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.LoginResponse;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.AuthService;
import com.example.user_service.service.RefreshTokenService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("🔍 AuthController.register() called with request: " + request);
        UUID userId = authService.register(request);
        if (userId == null) {
            throw new IllegalArgumentException("등록 실패");
        }
        System.out.println("✅ User registered successfully with ID: " + userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               @RequestHeader("X-Device-Id") String deviceId) {
        validateDeviceId(deviceId);
        UUID userId = authService.login(request);
        String accessToken = jwtUtil.issue(userId);
        String refreshToken = refreshTokenService.issue(userId, deviceId);
        return new LoginResponse(accessToken, refreshToken, userId);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(
            @RequestHeader("X-Refresh-Token") String refreshToken,
            @RequestHeader("X-Device-Id") String deviceId) {
        validateDeviceId(deviceId);
        validateRefreshToken(refreshToken);

        UUID userId = refreshTokenService.getUserIdFromToken(refreshToken, deviceId);

        String newAccessToken = jwtUtil.issue(userId);
        String newRefreshToken = refreshTokenService.issue(userId, deviceId);

        return new LoginResponse(newAccessToken, newRefreshToken, userId);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("X-Device-Id") String deviceId,
                                       Authentication authentication) {
        validateDeviceId(deviceId);
        UUID userId = (UUID) authentication.getPrincipal();
        refreshTokenService.revoke(userId, deviceId);
        return ResponseEntity.ok().build();
    }

    private void validateDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId 가 필요합니다.");
        }
    }

    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("리프레시 토큰이 필요합니다.");
        }
    }
}
