package com.example.user_service.controller;

import com.example.user_service.config.TestSecurityConfig;
import com.example.user_service.domain.User;
import com.example.user_service.dto.GroupCreateRequest;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtUtil;
import com.example.user_service.service.GroupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GroupController.class)
@Import(TestSecurityConfig.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private GroupService groupService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_USER_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Test
    @DisplayName("그룹 생성 성공")
    void createGroup_success() throws Exception {
        UUID userId = UUID.fromString(TEST_USER_ID);
        User user = createTestUser(userId, "testuser", "테스트유저");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(groupService.createGroup(any(UUID.class), any(GroupCreateRequest.class)))
                .thenReturn(UUID.randomUUID());

        String requestBody = """
            {
                "groupName": "테스트그룹",
                "goalAmount": 100000,
                "initialAmount": 0
            }
            """;

        mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("그룹 생성 실패 - 빈 그룹명")
    @WithMockUser(username = TEST_USER_ID)
    void createGroup_fail_emptyName() throws Exception {
        UUID userId = UUID.fromString(TEST_USER_ID);

        String requestBody = """
                {
                    "groupName": "",
                    "goalAmount": 1000000,
                    "initialAmount": 0
                }
                """;

        mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("그룹 목록 조회 성공")
    void getGroups_success() throws Exception {
        UUID userId = UUID.fromString(TEST_USER_ID);
        User user = createTestUser(userId, "testuser", "테스트유저");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(groupService.findMyGroups(any(UUID.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/groups/mine")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))))))
                .andExpect(status().isOk());
    }

    private User createTestUser(UUID userId, String username, String nickname) {
        return User.create("encodedPassword", username, nickname);
    }
}