package com.osgiliath.domain.exceptions;

import com.osgiliath.domain.shared.DomainException;

/** Exception thrown when a payment amount exceeds the invoice balance due. */
public class InsufficientBalanceException extends DomainException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
