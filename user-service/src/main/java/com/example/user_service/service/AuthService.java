package com.example.user_service.service;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UUID register(RegisterRequest request) {
        validatePasswordConfirm(request.password(), request.passwordConfirm());
        if (isDuplicate(request.username())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        String encoded = passwordEncoder.encode(request.password());
        User newUser = User.create(encoded, request.username(), request.nickname());
        User saved = userRepository.save(newUser);
        return saved.getUserId();
    }

    @Transactional(readOnly = true)
    public UUID login(LoginRequest request) {
        User user = userRepository.findByUserName(request.username())
                .orElseThrow(() -> new IllegalArgumentException("해당 이름으로 계정을 찾을 수 없습니다."));
        if (!isPasswordMatch(user, request.password())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return user.getUserId();
    }

    private boolean isDuplicate(String userName) {
        Optional<User> user = userRepository.findByUserName(userName);
        return user.isPresent();
    }

    private boolean isPasswordMatch(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
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
