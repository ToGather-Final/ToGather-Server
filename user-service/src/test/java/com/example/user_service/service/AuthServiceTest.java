package com.example.user_service.service;

import com.example.user_service.domain.User;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공")
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest("테스트유저", "testuser", "password123", "password123");
        UUID expectedUserId = UUID.randomUUID();
        User savedUser = createTestUser(expectedUserId, "testuser", "테스트유저");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UUID result = authService.register(request);

        assertThat(result).isEqualTo(expectedUserId);
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 아이디")
    void register_fail_duplicateUsername() throws Exception {
        RegisterRequest request = new RegisterRequest("테스트유저", "testuser", "password123", "password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 아이디입니다.");
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 불일치")
    void register_fail_passwordMismatch() throws Exception {
        RegisterRequest request = new RegisterRequest("테스트유저", "testuser", "password123", "different123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "password123");
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "testuser", "테스트유저");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        UUID result = authService.login(request);

        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("로그인 실패 - 사용자 없음")
    void login_fail_userNotFound() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "password123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 이름으로 계정을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_fail_wrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId, "testuser", "테스트유저");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
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