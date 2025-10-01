package com.example.user_service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GroupMemberAddRequest(@NotNull UUID userId) {
}
