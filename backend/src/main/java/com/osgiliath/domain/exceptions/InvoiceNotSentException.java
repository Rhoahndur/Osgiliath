package com.osgiliath.domain.exceptions;

import com.osgiliath.domain.shared.DomainException;

/**
 * Exception thrown when attempting to perform an operation that requires the invoice to be in SENT
 * or OVERDUE status, but it is in a different status.
 */
public class InvoiceNotSentException extends DomainException {
    public InvoiceNotSentException(String message) {
        super(message);
    }
}
