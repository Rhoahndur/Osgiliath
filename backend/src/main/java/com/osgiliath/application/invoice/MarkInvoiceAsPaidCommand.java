package com.osgiliath.application.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Command to manually mark an invoice as paid (administrative override)
 */
@AllArgsConstructor
@Getter
public class MarkInvoiceAsPaidCommand {
    private final UUID invoiceId;
}
