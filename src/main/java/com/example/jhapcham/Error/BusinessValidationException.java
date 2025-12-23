package com.example.jhapcham.Error;

/**
 * Exception thrown when business validation rules are violated.
 * This includes invalid input data, constraint violations, or business logic
 * failures.
 * Maps to HTTP 400 Bad Request.
 */
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }

    public BusinessValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
