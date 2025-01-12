package com.devita.common.exception;

import com.devita.common.response.ApiResponse;
import com.devita.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequestException(IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), request.getRequestURI());
        return new ResponseEntity<>(ApiResponse.error(errorResponse), ex.getErrorCode().getStatus());
    }

    // 403 Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), request.getRequestURI());
        return new ResponseEntity<>(ApiResponse.error(errorResponse), ex.getErrorCode().getStatus());
    }

    // 404 Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), request.getRequestURI());
        return new ResponseEntity<>(ApiResponse.error(errorResponse), ex.getErrorCode().getStatus());
    }

    // Security Error
    @ExceptionHandler(SecurityTokenException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFoundException(SecurityTokenException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), request.getRequestURI());
        return new ResponseEntity<>(ApiResponse.error(errorResponse), ex.getErrorCode().getStatus());
    }


    // 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleAllExceptions(Exception ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
        return new ResponseEntity<>(ApiResponse.error(errorResponse), ErrorCode.INTERNAL_SERVER_ERROR.getStatus());
    }
}