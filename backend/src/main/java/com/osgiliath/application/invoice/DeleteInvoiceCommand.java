package com.osgiliath.application.invoice;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Command to delete an invoice (DRAFT status only) */
@AllArgsConstructor
@Getter
public class DeleteInvoiceCommand {
    private final UUID invoiceId;
}
