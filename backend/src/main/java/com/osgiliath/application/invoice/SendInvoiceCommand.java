package com.osgiliath.application.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Command to send an invoice (DRAFT -> SENT transition)
 */
@AllArgsConstructor
@Getter
public class SendInvoiceCommand {
    private final UUID invoiceId;
}
