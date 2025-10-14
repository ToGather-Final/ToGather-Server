package com.example.user_service.controller;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.LoginResponse;
import com.example.user_service.dto.RefreshTokenRequest;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.AuthService;
import com.example.user_service.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
                                         @RequestHeader("X-Device-Id") String deviceId) {
        try {
            log.info("=== 회원가입 요청 시작 ===");
            log.info("요청 사용자명: {}", request.username());
            log.info("Device ID: {}", deviceId);
            
            validateDeviceId(deviceId);
            UUID userId = authService.register(request);
            if (userId == null) {
                log.error("회원가입 실패: userId가 null입니다.");
                throw new IllegalArgumentException("등록 실패");
            }

            log.info("회원가입 성공: userId={}", userId);
            String accessToken = jwtUtil.issue(userId);
            String refreshToken = refreshTokenService.issue(userId, deviceId);
            
            log.info("JWT 토큰 생성 완료");
            return ResponseEntity.status(HttpStatus.CREATED).body(new LoginResponse(accessToken, refreshToken, userId));
        } catch (Exception e) {
            log.error("=== 회원가입 실패 ===");
            log.error("에러 타입: {}", e.getClass().getName());
            log.error("에러 메시지: {}", e.getMessage());
            log.error("스택 트레이스:", e);
            throw e;
        }
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
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader("X-Device-Id") String deviceId) {
        validateDeviceId(deviceId);
        validateRefreshToken(request.refreshToken());

        UUID userId = refreshTokenService.getUserIdFromToken(request.refreshToken(), deviceId);

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
