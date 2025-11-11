package com.osgiliath.domain.exceptions;

/**
 * Exception thrown when a payment amount exceeds the invoice balance due.
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
