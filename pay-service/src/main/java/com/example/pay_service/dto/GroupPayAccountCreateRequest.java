package com.example.pay_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GroupPayAccountCreateRequest(
        @NotBlank(message = "이름을 입력해주세요")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다")
        String name,

        @NotBlank(message = "영문 성을 입력해주세요")
        @Size(max = 50, message = "영문 성은 50자 이하여야 합니다")
        @Pattern(regexp = "^[a-zA-Z]+$", message = "영문 성은 알파벳만 포함해야 합니다")
        String englishLastName,

        @NotBlank(message = "영문 이름을 입력해주세요")
        @Size(max = 50, message = "영문 이름은 50자 이하여야 합니다")
        @Pattern(regexp = "^[a-zA-Z]+$", message = "영문 이름은 알파벳만 포함해야 합니다")
        String englishFirstName,

        @NotBlank(message = "개인정보 처리 동의가 필요합니다")
        Boolean agreeToTerms
) {
}
