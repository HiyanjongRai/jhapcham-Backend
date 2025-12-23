package com.example.jhapcham.Error;

/**
 * Exception thrown when a requested resource cannot be found.
 * This includes entities like User, Product, Order, etc. that don't exist.
 * Maps to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
