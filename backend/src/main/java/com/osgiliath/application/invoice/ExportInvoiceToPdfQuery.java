package com.osgiliath.application.invoice;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Query to export an invoice as PDF */
@Getter
@AllArgsConstructor
public class ExportInvoiceToPdfQuery {
    private final UUID invoiceId;
}
