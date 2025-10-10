package com.example.user_service.controller;

import com.example.user_service.dto.MeResponse;
import com.example.user_service.dto.NicknameUpdateRequest;
import com.example.user_service.domain.User;
import com.example.user_service.repository.UserRepository;
import jakarta.validation.Valid;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return new MeResponse(user.getUserId(), user.getUsername(), user.getNickname());
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<MeResponse> updateNickname(@Valid @RequestBody NicknameUpdateRequest request, Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        validateNickname(request.nickname());
        user.changeNickname(request.nickname());
        MeResponse body = new MeResponse(user.getUserId(), user.getUsername(), user.getNickname());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/exists")
    public ResponseEntity<Map<String, Boolean>> checkUsernameExists(@RequestParam String username) {
        boolean exists = userRepository.existsByUsername(username);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/{userId}/nickname")
    public ResponseEntity<Map<String, String>> getUserNickname(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(Map.of("nickname", user.getNickname()));
    }

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임을 입력하세요.");
        }
        if (nickname.length() > 20) {
            throw new IllegalArgumentException("닉네임은 20자 이하여야 합니다.");
        }
    }
}
