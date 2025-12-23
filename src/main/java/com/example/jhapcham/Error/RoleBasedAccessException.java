package com.example.jhapcham.Error;

/**
 * Exception thrown when a user tries to access a resource that requires a
 * different role.
 * For example, a customer accessing seller-only endpoints or vice versa.
 * Maps to HTTP 403 Forbidden.
 */
public class RoleBasedAccessException extends RuntimeException {

    public RoleBasedAccessException(String message) {
        super(message);
    }

    public RoleBasedAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
