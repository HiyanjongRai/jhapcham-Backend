package com.example.jhapcham.Error;

import com.example.jhapcham.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
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
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiResponse<Void>> error(
            HttpStatus status,
            String errorCode,
            String message,
            Map<String, String> errors) {
        return ResponseEntity.status(status).body(ApiResponse.error(message, errorCode, errors));
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String errorCode, String message) {
        return error(status, errorCode, message, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "Incorrect email or password. Please try again.");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        logger.warn("User not found: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND",
                "Incorrect email or password. Please try again.");
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationException(AuthorizationException ex) {
        logger.warn("Authorization denied: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "AUTHORIZATION_DENIED",
                "You do not have permission to access this resource.");
    }

    @ExceptionHandler(RoleBasedAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoleBasedAccessException(RoleBasedAccessException ex) {
        logger.warn("Role-based access denied: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "ROLE_ACCESS_DENIED",
                "You do not have permission to perform this action.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You do not have permission to perform this action.");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                ex.getMessage() != null ? ex.getMessage() : "Requested resource not found.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        logger.warn("No route matched: {}", ex.getResourcePath());
        return error(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND",
                "No API route found for path: " + ex.getResourcePath());
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessValidationException(BusinessValidationException ex) {
        logger.warn("Business validation failed: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT",
                ex.getMessage() != null ? ex.getMessage() : "Invalid request.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        logger.warn("Validation errors: {}", errors);
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Please correct the highlighted fields and try again.", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        logger.warn("Constraint validation failed: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION",
                "Request contains invalid values.");
    }

    @ExceptionHandler(OrderStateConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderStateConflictException(OrderStateConflictException ex) {
        logger.warn("Order state conflict: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "ORDER_STATE_CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        logger.warn("Illegal state: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "CONFLICT",
                ex.getMessage() != null ? ex.getMessage() : "The request conflicts with the current state.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        logger.warn("Database constraint violation: {}", ex.getMostSpecificCause().getMessage());
        return error(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "The request conflicts with existing or invalid data.");
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseLockException(PessimisticLockingFailureException ex) {
        logger.warn("Database lock contention: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, "RESOURCE_BUSY",
                "The resource is being updated. Please retry shortly.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        logger.warn("HTTP method not supported: {}", ex.getMethod());
        return error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "HTTP method not allowed for this endpoint.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestParam(MissingServletRequestParameterException ex) {
        logger.warn("Missing request parameter: {}", ex.getParameterName());
        return error(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                String.format("Required request parameter '%s' is missing.", ex.getParameterName()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        logger.warn("No route matched: {}", ex.getRequestURL());
        return error(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND",
                String.format("No API route found for path: %s", ex.getRequestURL()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        logger.error("Unexpected runtime exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "Unable to process request. Please try again.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "Something went wrong. Please try again later.");
    }
}
