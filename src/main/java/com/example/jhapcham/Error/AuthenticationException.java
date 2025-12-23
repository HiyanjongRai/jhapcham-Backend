package com.example.jhapcham.Error;

/**
 * Exception thrown when authentication fails.
 * This includes wrong credentials, invalid tokens, or expired sessions.
 * Maps to HTTP 401 Unauthorized.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
