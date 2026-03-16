package com.osgiliath.domain.exceptions;

import com.osgiliath.domain.shared.DomainException;

/** Exception thrown when attempting to send an invoice that has no line items. */
public class InvoiceHasNoLineItemsException extends DomainException {
    public InvoiceHasNoLineItemsException(String message) {
        super(message);
    }
}
