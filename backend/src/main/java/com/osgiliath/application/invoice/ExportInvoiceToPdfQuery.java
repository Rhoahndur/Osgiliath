package com.osgiliath.application.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Query to export an invoice as PDF
 */
@Getter
@AllArgsConstructor
public class ExportInvoiceToPdfQuery {
    private final UUID invoiceId;
}
