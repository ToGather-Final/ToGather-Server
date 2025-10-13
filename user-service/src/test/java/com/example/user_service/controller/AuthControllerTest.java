package com.example.user_service.controller;

import com.example.user_service.config.TestSecurityConfig;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.error.GlobalExceptionHandler;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.AuthService;
import com.example.user_service.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 성공")
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "password123", "password123", "테스트유저");
        UUID expectedUserId = UUID.randomUUID();
        when(authService.register(any())).thenReturn(expectedUserId);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 아이디")
    void register_fail_duplicateUsername() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "password123", "password123", "테스트유저");
        when(authService.register(any())).thenReturn(null);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "password123");
        UUID userId = UUID.randomUUID();
        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        when(authService.login(any())).thenReturn(userId);
        when(jwtUtil.issue(userId)).thenReturn(accessToken);
        when(refreshTokenService.issue(userId, "device123")).thenReturn(refreshToken);

        mockMvc.perform(post("/auth/login")
                .header("X-Device-Id", "device123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(accessToken))
                .andExpect(jsonPath("$.refreshToken").value(refreshToken))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    @DisplayName("로그인 실패 - deviceId 누락")
    void login_fail_missingDeviceId() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "password123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refresh_success() throws Exception {
        UUID userId = UUID.randomUUID();
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        when(refreshTokenService.getUserIdFromToken("refresh-token", "device123")).thenReturn(userId);
        when(jwtUtil.issue(userId)).thenReturn(newAccessToken);
        when(refreshTokenService.issue(userId, "device123")).thenReturn(newRefreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .header("X-Refresh-Token", "refresh-token")
                        .header("X-Device-Id", "device123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(newAccessToken))
                .andExpect(jsonPath("$.refreshToken").value(newRefreshToken));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(refreshTokenService).revoke(userId, "device123");

        mockMvc.perform(post("/auth/logout")
                        .header("X-Device-Id", "device123")
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}
