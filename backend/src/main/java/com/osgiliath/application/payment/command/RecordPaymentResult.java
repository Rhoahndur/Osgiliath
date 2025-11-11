package com.osgiliath.application.payment.command;

import com.osgiliath.domain.invoice.InvoiceStatus;
import com.osgiliath.domain.shared.Money;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * Result of recording a payment
 * Contains both payment and updated invoice information
 */
@Data
@AllArgsConstructor
public class RecordPaymentResult {

    private final UUID paymentId;
    private final UUID invoiceId;
    private final Money paymentAmount;
    private final Money updatedBalance;
    private final InvoiceStatus updatedInvoiceStatus;
}
