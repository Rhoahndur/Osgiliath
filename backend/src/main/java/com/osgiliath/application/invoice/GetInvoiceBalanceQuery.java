package com.osgiliath.application.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Query to get an invoice's balance information
 */
@Getter
@AllArgsConstructor
public class GetInvoiceBalanceQuery {
    private final UUID invoiceId;
}
