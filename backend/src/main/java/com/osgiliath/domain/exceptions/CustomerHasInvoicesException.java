package com.osgiliath.domain.exceptions;

/**
 * Exception thrown when attempting to delete a customer that has existing invoices.
 */
public class CustomerHasInvoicesException extends RuntimeException {
    public CustomerHasInvoicesException(String message) {
        super(message);
    }
}
