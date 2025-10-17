package com.example.user_service.controller;

import com.example.user_service.dto.MeResponse;
import com.example.user_service.dto.NicknameUpdateRequest;
import com.example.user_service.domain.User;
import com.example.user_service.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Tag(name = "사용자 관리", description = "사용자 정보 조회, 수정, 중복 확인 관련 API")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        UUID userId = (UUID) authentication.getPrincipal();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return new MeResponse(user.getUserId(), user.getUsername(), user.getNickname());
    }

    @Operation(summary = "닉네임 수정", description = "현재 사용자의 닉네임을 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "닉네임 수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PatchMapping("/me/nickname")
    public ResponseEntity<MeResponse> updateNickname(
            @Parameter(description = "닉네임 수정 요청 데이터", required = true) @Valid @RequestBody NicknameUpdateRequest request, 
            Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        UUID userId = (UUID) authentication.getPrincipal();

        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        validateNickname(request.nickname());
        user.changeNickname(request.nickname());
        MeResponse body = new MeResponse(user.getUserId(), user.getUsername(), user.getNickname());
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "사용자명 중복 확인", description = "특정 사용자명이 이미 존재하는지 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자명 중복 확인 성공")
    })
    @GetMapping("/exists")
    public ResponseEntity<Map<String, Boolean>> checkUsernameExists(
            @Parameter(description = "확인할 사용자명", required = true) @RequestParam String username) {
        boolean exists = userRepository.existsByUsername(username);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @Operation(summary = "사용자 닉네임 조회", description = "특정 사용자의 닉네임을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "닉네임 조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{userId}/nickname")
    public ResponseEntity<Map<String, String>> getUserNickname(
            @Parameter(description = "사용자 ID", required = true) @PathVariable UUID userId) {
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
