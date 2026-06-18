package com.example.jhapcham.Error;

/**
 * Exception thrown when an order action conflicts with the current persisted
 * order state, such as processing an already cancelled order.
 */
public class OrderStateConflictException extends RuntimeException {

    public OrderStateConflictException(String message) {
        super(message);
    }
}
