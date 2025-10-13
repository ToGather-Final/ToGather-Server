package com.example.user_service.error;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        ApiError body = new ApiError("VALIDATION_ERROR", message, request.getRequestURI(),
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        String code = "BUSINESS_ERROR";
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (isAuthenticationError(e.getMessage())) {
            code = "AUTHENTICATION_ERROR";
            status = HttpStatus.UNAUTHORIZED;
        }

        ApiError body = new ApiError(code, e.getMessage(), request.getRequestURI(),
                LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleEtc(Exception e, HttpServletRequest request) {
        ApiError body = new ApiError("INTERNAL_ERROR", "일시적인 오류가 발생했습니다.", request.getRequestURI(),
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException e, HttpServletRequest request) {
        ApiError body = new ApiError("FORBIDDEN", e.getMessage(), request.getRequestURI(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(NoSuchElementException e, HttpServletRequest request) {
        ApiError body = new ApiError("NOT_FOUND", e.getMessage(), request.getRequestURI(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    private boolean isAuthenticationError(String message) {
        return message.contains("아이디 또는 비밀번호") ||
                message.contains("인증") ||
                message.contains("토큰") ||
                message.contains("로그인");
    }
}
