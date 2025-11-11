package com.osgiliath.application.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to update an invoice (DRAFT only)
 */
@Getter
@AllArgsConstructor
public class UpdateInvoiceCommand {
    private final UUID invoiceId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
}
