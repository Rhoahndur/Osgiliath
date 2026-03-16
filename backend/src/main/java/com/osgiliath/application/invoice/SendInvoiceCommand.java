package com.osgiliath.application.invoice;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Command to send an invoice (DRAFT -> SENT transition) */
@AllArgsConstructor
@Getter
public class SendInvoiceCommand {
    private final UUID invoiceId;
}
