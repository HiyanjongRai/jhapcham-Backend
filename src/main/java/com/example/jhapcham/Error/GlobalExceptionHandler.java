package com.example.jhapcham.Error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the entire application.
 * Catches all exceptions and returns standardized error responses.
 * Prevents the application from crashing and provides user-friendly error
 * messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ErrorResponse error(HttpStatus status, String errorCode, String message, WebRequest request) {
        return new ErrorResponse(
                status.value(),
                status.name(),
                errorCode,
                message,
                request.getDescription(false).replace("uri=", ""));
    }

    // ==================== AUTHENTICATION ERRORS (401) ====================

    /**
     * Handle authentication failures (wrong credentials, invalid tokens)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse error = error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                ex.getMessage() != null ? ex.getMessage() : "Incorrect email or password. Please try again.", request);
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle Spring Security's UsernameNotFoundException
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(
            UsernameNotFoundException ex, WebRequest request) {
        logger.warn("User not found: {}", ex.getMessage());
        ErrorResponse error = error(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND",
                "Incorrect email or password. Please try again.", request);
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
        ErrorResponse error = error(HttpStatus.FORBIDDEN, "AUTHORIZATION_DENIED",
                ex.getMessage() != null ? ex.getMessage() : "You are not authorized to access this resource.", request);
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle role-based access violations
     */
    @ExceptionHandler(RoleBasedAccessException.class)
    public ResponseEntity<ErrorResponse> handleRoleBasedAccessException(
            RoleBasedAccessException ex, WebRequest request) {
        logger.warn("Role-based access denied: {}", ex.getMessage());
        ErrorResponse error = error(HttpStatus.FORBIDDEN, "ROLE_ACCESS_DENIED",
                ex.getMessage() != null ? ex.getMessage() : "You do not have permission to perform this action.", request);
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle Spring Security's AccessDeniedException
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        logger.warn("Access denied: {}", ex.getMessage());
        ErrorResponse error = error(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You do not have permission to perform this action.", request);
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
        ErrorResponse error = error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                ex.getMessage() != null ? ex.getMessage() : "Requested resource not found.", request);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle unmatched MVC routes that fall through static resource handling.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException ex, WebRequest request) {
        logger.warn("No route matched: {}", ex.getResourcePath());
        ErrorResponse error = error(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND",
                "No API route found for path: " + ex.getResourcePath(), request);
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
        ErrorResponse error = error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage(), request);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        ErrorResponse error = error(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), request);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle Spring validation errors (from @Valid annotations)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse response = error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Validation failed for one or more fields", request);
        response.setErrors(errors);

        logger.warn("Validation errors: {}", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        logger.warn("Constraint validation failed: {}", ex.getMessage());
        ErrorResponse error = error(HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION",
                "Request contains invalid values.", request);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // ==================== CONFLICT ERRORS (409) ====================

    @ExceptionHandler(OrderStateConflictException.class)
    public ResponseEntity<ErrorResponse> handleOrderStateConflictException(
            OrderStateConflictException ex, WebRequest request) {
        logger.warn("Order state conflict: {}", ex.getMessage());
        ErrorResponse error = error(HttpStatus.CONFLICT, "ORDER_STATE_CONFLICT", ex.getMessage(), request);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Handle illegal state exceptions (conflict situations)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        logger.warn("Illegal state: {}", ex.getMessage());
        ErrorResponse error = error(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        logger.warn("Database constraint violation: {}", ex.getMostSpecificCause().getMessage());
        ErrorResponse error = error(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "The request conflicts with existing or invalid data.", request);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseLockException(
            RuntimeException ex, WebRequest request) {
        logger.warn("Database lock contention: {}", ex.getMessage());
        ErrorResponse error = error(HttpStatus.CONFLICT, "RESOURCE_BUSY",
                "The resource is being updated. Please retry shortly.", request);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {
        logger.warn("HTTP method not supported: {}", ex.getMethod());
        ErrorResponse error = error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                ex.getMessage() != null ? ex.getMessage() : "HTTP method not allowed for this endpoint.", request);
        return new ResponseEntity<>(error, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestParam(
            MissingServletRequestParameterException ex, WebRequest request) {
        logger.warn("Missing request parameter: {}", ex.getParameterName());
        ErrorResponse error = error(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                String.format("Required request parameter '%s' is missing.", ex.getParameterName()), request);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {
        logger.warn("No route matched: {}", ex.getRequestURL());
        ErrorResponse error = error(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND",
                String.format("No API route found for path: %s", ex.getRequestURL()), request);
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
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
        ErrorResponse error = error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "Something went wrong. Please try again later.", request);
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
