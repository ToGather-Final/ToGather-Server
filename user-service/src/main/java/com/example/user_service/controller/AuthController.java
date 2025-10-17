package com.example.user_service.controller;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.LoginResponse;
import com.example.user_service.dto.RefreshTokenRequest;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.AuthService;
import com.example.user_service.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "인증 관리", description = "회원가입, 로그인, 토큰 갱신, 로그아웃 관련 API")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "회원가입", description = "새로운 사용자 계정을 생성하고 액세스 토큰과 리프레시 토큰을 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "회원가입 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 중복된 사용자명"),
        @ApiResponse(responseCode = "401", description = "디바이스 ID 누락")
    })
    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> register(
            @Parameter(description = "회원가입 요청 데이터", required = true) @Valid @RequestBody RegisterRequest request,
            @Parameter(description = "디바이스 ID", required = true) @RequestHeader("X-Device-Id") String deviceId) {
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

    @Operation(summary = "로그인", description = "사용자 인증 후 액세스 토큰과 리프레시 토큰을 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패 또는 디바이스 ID 누락")
    })
    @PostMapping("/login")
    public LoginResponse login(
            @Parameter(description = "로그인 요청 데이터", required = true) @Valid @RequestBody LoginRequest request,
            @Parameter(description = "디바이스 ID", required = true) @RequestHeader("X-Device-Id") String deviceId) {
        validateDeviceId(deviceId);
        UUID userId = authService.login(request);
        String accessToken = jwtUtil.issue(userId);
        String refreshToken = refreshTokenService.issue(userId, deviceId);
        return new LoginResponse(accessToken, refreshToken, userId);
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰과 리프레시 토큰을 발급합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰 또는 디바이스 ID 누락")
    })
    @PostMapping("/refresh")
    public LoginResponse refresh(
            @Parameter(description = "리프레시 토큰", required = true) @RequestHeader("X-Refresh-Token") String refreshToken,
            @Parameter(description = "디바이스 ID", required = true) @RequestHeader("X-Device-Id") String deviceId) {
        validateDeviceId(deviceId);
        validateRefreshToken(refreshToken);

        UUID userId = refreshTokenService.getUserIdFromToken(refreshToken, deviceId);

        String newAccessToken = jwtUtil.issue(userId);
        String newRefreshToken = refreshTokenService.issue(userId, deviceId);

        return new LoginResponse(newAccessToken, newRefreshToken, userId);
    }

    @Operation(summary = "로그아웃", description = "사용자의 리프레시 토큰을 무효화하여 로그아웃을 처리합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패 또는 디바이스 ID 누락")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "디바이스 ID", required = true) @RequestHeader("X-Device-Id") String deviceId,
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
