package com.example.user_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GroupRuleUpdateRequest(
        @NotNull @Positive Integer voteQuorum,
        @NotNull @Positive Integer voteDurationHours
) {
}
