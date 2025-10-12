package com.example.pay_service.dto;

import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        List<String> hints,
        List<AlternativeAccount> alternativeAccounts
) {
    public record AlternativeAccount(
            String accountId,
            String nickname,
            Long balance
    ) {}

    public static ErrorResponse insufficientFunds(String message, List<AlternativeAccount> alternativeAccounts) {
        List<String> hints = List.of("임금 후 재시도");
        return new ErrorResponse("INSUFFICIENT_FUNDS", message, hints, alternativeAccounts);
    }

    public static ErrorResponse tokenInvalid(String message) {
        return new ErrorResponse("TOKEN_INVALID", message, null, null);
    }

    public static ErrorResponse tokenExpired(String message) {
        return new ErrorResponse("TOKEN_EXPIRED", message, null, null);
    }

    public static ErrorResponse sessionNotFound(String message) {
        return new ErrorResponse("SESSION_NOT_FOUND", message, null, null);
    }

    public static ErrorResponse sessionUsed(String message) {
        return new ErrorResponse("SESSION_USED", message, null, null);
    }

    public static ErrorResponse accountNotOwned(String message) {
        return new ErrorResponse("ACCOUNT_NOT_OWNED", message, null, null);
    }

    public static ErrorResponse idempotent(String message) {
        return new ErrorResponse("IDEMPOTENT_REPLAY", message, null, null);
    }
}
