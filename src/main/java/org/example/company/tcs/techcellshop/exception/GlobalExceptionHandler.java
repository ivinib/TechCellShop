package org.example.company.tcs.techcellshop.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.example.company.tcs.techcellshop.domain.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler({
            InsufficientStockException.class,
            CouponValidationException.class,
            InvalidOrderStatusTransitionException.class,
            ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(Exception ex, HttpServletRequest request) {
        String message = (ex instanceof ObjectOptimisticLockingFailureException)
                ? "The resource was modified by another request. Please try again."
                : ex.getMessage();
        return build(HttpStatus.CONFLICT, "BUSINESS_CONFLICT", message, request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected internal error", request, null);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            Map<String, String> validationErrors
    ) {
        String traceId = (String) request.getAttribute("traceId");
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                traceId,
                validationErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}