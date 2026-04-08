package org.example.company.tcs.techcellshop.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.example.company.tcs.techcellshop.domain.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Validation failed",
                request.getRequestURI(),
                resolveTraceId(request),
                validationErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v ->
                validationErrors.put(v.getPropertyPath().toString(), v.getMessage())
        );

        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Validation failed",
                request.getRequestURI(),
                resolveTraceId(request),
                validationErrors
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                safeMessage(ex.getMessage(), "Resource not found"),
                request.getRequestURI(),
                resolveTraceId(request),
                null
        );
    }

    @ExceptionHandler({
            InvalidOrderStatusTransitionException.class,
            CouponValidationException.class,
            InsufficientStockException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex, HttpServletRequest request) {
        return build(
                HttpStatus.CONFLICT,
                "BUSINESS_CONFLICT",
                safeMessage(ex.getMessage(), "Business conflict"),
                request.getRequestURI(),
                resolveTraceId(request),
                null
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_ARGUMENT",
                safeMessage(ex.getMessage(), "Invalid request"),
                request.getRequestURI(),
                resolveTraceId(request),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Internal server error",
                request.getRequestURI(),
                resolveTraceId(request),
                null
        );
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, String path, String traceId, Map<String, String> validationErrors) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                traceId,
                validationErrors
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ErrorResponse> handleRequestBinding(ServletRequestBindingException ex, HttpServletRequest request) {
        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_ARGUMENT",
                safeMessage(ex.getMessage(), "Invalid request"),
                request.getRequestURI(),
                resolveTraceId(request),
                null
        );
    }


    private String resolveTraceId(HttpServletRequest request) {
        Object traceId = request.getAttribute("traceId");
        return traceId instanceof String value && StringUtils.hasText(value) ? value : null;
    }

    private String safeMessage(String candidate, String fallback) {
        return StringUtils.hasText(candidate) ? candidate : fallback;
    }
}