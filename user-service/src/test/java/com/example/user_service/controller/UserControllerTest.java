package com.example.user_service.controller;

import com.example.user_service.config.TestSecurityConfig;
import com.example.user_service.dto.NicknameUpdateRequest;
import com.example.user_service.domain.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_USER_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Test
    @DisplayName("내 정보 조회 성공")
    @WithMockUser(username = TEST_USER_ID)
    void me_success() throws Exception {
        UUID userId = UUID.fromString(TEST_USER_ID);
        User user = createTestUser(userId, "testuser", "테스트유저");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.nickname").value("테스트유저"));
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 사용자 없음")
    @WithMockUser(username = TEST_USER_ID)
    void me_fail_userNotFound() throws Exception {
        UUID userId = UUID.fromString(TEST_USER_ID);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("닉네임 수정 성공")
    @WithMockUser(username = TEST_USER_ID)
    void updateNickname_success() throws Exception {
        UUID userId = UUID.fromString(TEST_USER_ID);
        User user = createTestUser(userId, "testuser", "기존닉네임");

        NicknameUpdateRequest request = new NicknameUpdateRequest("새닉네임");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(patch("/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("새닉네임"));
    }

    @Test
    @DisplayName("닉네임 수정 실패 - 빈 닉네임")
    @WithMockUser(username = TEST_USER_ID)
    void updateNickname_fail_emptyNickname() throws Exception {
        NicknameUpdateRequest request = new NicknameUpdateRequest("");

        mockMvc.perform(patch("/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("아이디 중복 확인 - 존재함")
    void checkUsernameExists_true() throws Exception {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        mockMvc.perform(get("/users/exists")
                        .param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    @DisplayName("아이디 중복 확인 - 존재하지 않음")
    void checkUsernameExists_false() throws Exception {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        mockMvc.perform(get("/users/exists")
                        .param("username", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }

    private User createTestUser(UUID userId, String username, String nickname) {
        return User.builder()
                .userId(userId)
                .username(username)
                .nickname(nickname)
                .password("encodedPassword")
                .createdAt(LocalDateTime.now())
                .build();
    }
}