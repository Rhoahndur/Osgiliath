package com.osgiliath.domain.exceptions;

/**
 * Exception thrown when attempting to send an invoice that has no line items.
 */
public class InvoiceHasNoLineItemsException extends RuntimeException {
    public InvoiceHasNoLineItemsException(String message) {
        super(message);
    }
}
