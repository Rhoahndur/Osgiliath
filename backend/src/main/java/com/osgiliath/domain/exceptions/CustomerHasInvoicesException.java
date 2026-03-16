package com.osgiliath.domain.exceptions;

import com.osgiliath.domain.shared.DomainException;

/** Exception thrown when attempting to delete a customer that has existing invoices. */
public class CustomerHasInvoicesException extends DomainException {
    public CustomerHasInvoicesException(String message) {
        super(message);
    }
}
