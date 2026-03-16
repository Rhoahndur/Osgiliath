package com.osgiliath.application.invoice;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Command to manually mark an invoice as paid (administrative override) */
@AllArgsConstructor
@Getter
public class MarkInvoiceAsPaidCommand {
    private final UUID invoiceId;
}
