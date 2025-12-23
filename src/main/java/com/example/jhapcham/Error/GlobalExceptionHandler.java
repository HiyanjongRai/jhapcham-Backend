package com.example.jhapcham.Error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the entire application.
 * Catches all exceptions and returns standardized error responses.
 * Prevents the application from crashing and provides user-friendly error
 * messages.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ==================== AUTHENTICATION ERRORS (401) ====================

    /**
     * Handle authentication failures (wrong credentials, invalid tokens)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("AUTHENTICATION_FAILED",
                ex.getMessage() != null ? ex.getMessage() : "Incorrect email or password. Please try again.");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle Spring Security's UsernameNotFoundException
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(
            UsernameNotFoundException ex, WebRequest request) {
        logger.warn("User not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("USER_NOT_FOUND",
                "Incorrect email or password. Please try again.");
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // ==================== AUTHORIZATION ERRORS (403) ====================

    /**
     * Handle authorization failures (insufficient permissions, blocked accounts)
     */
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationException(
            AuthorizationException ex, WebRequest request) {
        logger.warn("Authorization denied: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("AUTHORIZATION_DENIED",
                ex.getMessage() != null ? ex.getMessage() : "You are not authorized to access this resource.");
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle role-based access violations
     */
    @ExceptionHandler(RoleBasedAccessException.class)
    public ResponseEntity<ErrorResponse> handleRoleBasedAccessException(
            RoleBasedAccessException ex, WebRequest request) {
        logger.warn("Role-based access denied: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("ROLE_ACCESS_DENIED",
                ex.getMessage() != null ? ex.getMessage() : "You do not have permission to perform this action.");
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle Spring Security's AccessDeniedException
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        logger.warn("Access denied: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("ACCESS_DENIED",
                "You do not have permission to perform this action.");
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // ==================== RESOURCE NOT FOUND (404) ====================

    /**
     * Handle resource not found errors
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        logger.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("RESOURCE_NOT_FOUND",
                ex.getMessage() != null ? ex.getMessage() : "Requested resource not found.");
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // ==================== VALIDATION ERRORS (400) ====================

    /**
     * Handle business validation failures
     */
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessValidationException(
            BusinessValidationException ex, WebRequest request) {
        logger.warn("Business validation failed: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("VALIDATION_FAILED", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("INVALID_ARGUMENT", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle Spring validation errors (from @Valid annotations)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", "VALIDATION_ERROR");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        response.put("errors", errors);
        response.put("message", "Validation failed for one or more fields");

        logger.warn("Validation errors: {}", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ==================== CONFLICT ERRORS (409) ====================

    /**
     * Handle illegal state exceptions (conflict situations)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        logger.warn("Illegal state: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("CONFLICT", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // ==================== SERVER ERRORS (500) ====================

    /**
     * Handle all other uncaught exceptions
     * This is the safety net to prevent the application from crashing
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        logger.error("Unexpected error occurred: ", ex);
        ErrorResponse error = new ErrorResponse("INTERNAL_SERVER_ERROR",
                "Something went wrong. Please try again later.");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
