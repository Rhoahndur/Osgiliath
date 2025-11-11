package com.osgiliath.application.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Command to delete an invoice (DRAFT status only)
 */
@AllArgsConstructor
@Getter
public class DeleteInvoiceCommand {
    private final UUID invoiceId;
}
