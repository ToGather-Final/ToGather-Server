package com.example.user_service.auth;

import com.example.user_service.controller.AuthController;
import com.example.user_service.dto.LoginResponse;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.AuthService;
import com.example.user_service.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerWithoutInfra {

    @Autowired
    MockMvc mvc;

    @MockBean
    AuthService authService;

    @MockBean
    JwtUtil jwtUtil;

    @MockitoBean
    RefreshTokenService refreshTokenService;


    @Test
    void 로그인_성공_액세스_리프레시_반환() throws Exception{
        UUID userId = UUID.randomUUID();
        String deviceId = "test-device-123";

        given(authService.login(any())).willReturn(userId);

        given(jwtUtil.issue(userId)).willReturn("access-xxx");
        given(refreshTokenService.issue(eq(userId),any(String.class))).willReturn("refresh-yyy");

        mvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .header("X-Device-Id",deviceId)
                        .content("""
                                  {"username": "a@test.com","password": "P@ssw0rd!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-xxx"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-yyy"));
    }
}
