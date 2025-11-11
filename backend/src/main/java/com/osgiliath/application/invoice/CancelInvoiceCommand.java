package com.osgiliath.application.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Command to cancel an invoice
 * Can be executed on DRAFT or SENT invoices
 */
@AllArgsConstructor
@Getter
public class CancelInvoiceCommand {
    private final UUID invoiceId;
    private final String reason; // Optional cancellation reason
}
