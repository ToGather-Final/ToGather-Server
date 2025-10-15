package com.example.user_service.service;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.domain.User;
import com.example.user_service.repository.UserRepository;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UUID register(RegisterRequest request) {
        try {
            log.info("AuthService.register 시작 - username: {}", request.username());
            
            if (isDuplicate(request.username())) {
                log.warn("회원가입 실패: 이미 존재하는 아이디 - {}", request.username());
                throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
            }

            log.info("비밀번호 유효성 검사 시작");
            validatePassword(request.password());
            validatePasswordConfirm(request.password(), request.passwordConfirm());

            log.info("비밀번호 인코딩 시작");
            String encoded = passwordEncoder.encode(request.password());
            
            log.info("User 엔티티 생성 및 저장");
            User newUser = User.create(encoded, request.username(), request.nickname());
            User saved = userRepository.save(newUser);
            
            log.info("회원가입 성공: userId={}", saved.getUserId());
            return saved.getUserId();
        } catch (Exception e) {
            log.error("AuthService.register 실패 - username: {}, 에러: {}", request.username(), e.getMessage(), e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public UUID login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!isPasswordMatch(user, request.password())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return user.getUserId();
    }

    private boolean isDuplicate(String username) {
        return userRepository.existsByUsername(username);
    }

    private boolean isPasswordMatch(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    private void validatePassword(String password){
        if (password == null || password.length() < 8){
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }
        if (!password.matches(".*[A-Za-z].*")) {
            throw new IllegalArgumentException("비밀번호는 영문자를 포함해야 합니다.");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("비밀번호는 숫자를 포함해야 합니다.");
        }
    }

    private void validatePasswordConfirm(String password, String confirm) {
        if (password == null) {
            throw new IllegalArgumentException("비밀번호가 비었습니다.");
        }
        if (!password.equals(confirm)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
    }
}
