package com.osgiliath.domain.exceptions;

/**
 * Exception thrown when attempting to perform an operation that requires
 * the invoice to be in SENT or OVERDUE status, but it is in a different status.
 */
public class InvoiceNotSentException extends RuntimeException {
    public InvoiceNotSentException(String message) {
        super(message);
    }
}
