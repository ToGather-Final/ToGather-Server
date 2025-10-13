package com.example.pay_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.pay_service.dto.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(PayServiceException.class)
    public ResponseEntity<ErrorResponse> handlePayServiceException(PayServiceException e) {
        log.error("PayService 예외 발생: code={}, message={}", e.getCode(), e.getMessage());

        ErrorResponse response = new ErrorResponse(
                e.getCode(),
                e.getMessage(),
                null,
                null
        );

        HttpStatus status = getHttpStatus(e.getCode());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(InsufficientFundsException e) {
        log.error("잔액 부족 예외: {}", e.getMessage());

        ErrorResponse response = ErrorResponse.insufficientFunds(e.getMessage(), null);
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
    }

    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<ErrorResponse> handleTokenInvalidException(TokenInvalidException e) {
        log.error("토큰 무효 예외: {}", e.getMessage());

        ErrorResponse response = ErrorResponse.tokenInvalid(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFoundException(SessionNotFoundException e) {
        log.error("세션 없음 예외: {}", e.getMessage());

        ErrorResponse response = ErrorResponse.sessionNotFound(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AccountNotOwnedException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotOwnedException(AccountNotOwnedException e) {
        log.error("계좌 소유권 예외: {}", e.getMessage());

        ErrorResponse response = ErrorResponse.accountNotOwned(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("예상치 못한 예외 발생: {}", e.getMessage());

        ErrorResponse response = new ErrorResponse(
                "INTERNAL_ERROR",
                "Internal server error",
                null,
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException e) {
        log.error("인증 실패 예외: {}", e.getMessage());

        ErrorResponse response = new ErrorResponse(
                "UNAUTHORIZED",
                e.getMessage(),
                null,
                null
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    private HttpStatus getHttpStatus(String code) {
        return switch (code) {
            case "TOKEN_INVALID" -> HttpStatus.BAD_REQUEST;
            case "TOKEN_EXPIRED" -> HttpStatus.GONE;
            case "SESSION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "SESSION_USED" -> HttpStatus.CONFLICT;
            case "INSUFFICIENT_FUNDS" -> HttpStatus.PAYMENT_REQUIRED;
            case "ACCOUNT_NOT_OWNED" -> HttpStatus.FORBIDDEN;
            case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
            case "IDEMPOTENT_REPLAY" -> HttpStatus.OK;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
