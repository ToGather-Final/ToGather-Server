package com.example.module_common.dto;

import java.util.UUID;

public record UserInfo(
        UUID userId,
        String name
) {
    public UserInfo {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 null일 수 없습니다");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 null이거나 빈 문자열일 수 없습니다");
        }
    }
}
