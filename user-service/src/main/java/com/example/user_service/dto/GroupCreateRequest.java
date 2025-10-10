package com.example.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record GroupCreateRequest(
        @NotBlank String groupName,
        @NotNull @Positive Integer goalAmount,
        @PositiveOrZero Integer initialAmount
) {
}
