package com.example.user_service.repository;

import com.example.user_service.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("사용자명으로 사용자 찾기 성공")
    void findByUsername_success() {
        // given
        User user = createTestUser("testuser", "테스트유저");
        entityManager.persistAndFlush(user);

        // when
        Optional<User> result = userRepository.findByUsername("testuser");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("사용자명으로 사용자 찾기 실패 - 존재하지 않는 사용자")
    void findByUsername_fail_userNotFound() {
        // when
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자명 중복 확인 - 존재함")
    void existsByUsername_true() {
        // given
        User user = createTestUser("testuser", "테스트유저");
        entityManager.persistAndFlush(user);

        // when
        boolean result = userRepository.existsByUsername("testuser");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("사용자명 중복 확인 - 존재하지 않음")
    void existsByUsername_false() {
        // when
        boolean result = userRepository.existsByUsername("nonexistent");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("사용자 ID로 사용자 찾기 성공")
    void findById_success() {
        // given
        User user = createTestUser("testuser", "테스트유저");
        entityManager.persistAndFlush(user);

        // when
        Optional<User> result = userRepository.findById(user.getUserId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(user.getUserId());
    }

    @Test
    @DisplayName("사용자 ID로 사용자 찾기 실패 - 존재하지 않는 ID")
    void findById_fail_userNotFound() {
        // given
        UUID nonExistentId = UUID.randomUUID();

        // when
        Optional<User> result = userRepository.findById(nonExistentId);

        // then
        assertThat(result).isEmpty();
    }

    private User createTestUser(String username, String nickname) {
        return User.create("encodedPassword", username, nickname);
    }
}