package com.example.jhapcham.Error;

/**
 * Exception thrown when a user tries to access a resource they don't have
 * permission for.
 * This includes not being logged in, insufficient permissions, or account
 * status issues.
 * Maps to HTTP 403 Forbidden.
 */
public class AuthorizationException extends RuntimeException {

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
