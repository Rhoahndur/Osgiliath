package com.osgiliath.application.invoice;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Command to cancel an invoice Can be executed on DRAFT or SENT invoices */
@AllArgsConstructor
@Getter
public class CancelInvoiceCommand {
    private final UUID invoiceId;
    private final String reason; // Optional cancellation reason
}
