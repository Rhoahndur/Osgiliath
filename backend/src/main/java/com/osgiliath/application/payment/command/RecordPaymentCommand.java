package com.osgiliath.application.payment.command;

import com.osgiliath.domain.payment.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to record a payment against an invoice
 */
@Getter
@AllArgsConstructor
public class RecordPaymentCommand {

    private final UUID invoiceId;
    private final BigDecimal amount;
    private final LocalDate paymentDate;
    private final PaymentMethod paymentMethod;
    private final String referenceNumber;
}
