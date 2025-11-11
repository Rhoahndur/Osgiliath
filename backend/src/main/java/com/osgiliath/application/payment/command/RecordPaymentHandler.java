package com.osgiliath.application.payment.command;

import com.osgiliath.domain.exceptions.InsufficientBalanceException;
import com.osgiliath.domain.exceptions.InvoiceNotSentException;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.invoice.InvoiceStatus;
import com.osgiliath.domain.payment.Payment;
import com.osgiliath.domain.payment.PaymentRepository;
import com.osgiliath.domain.shared.DomainException;
import com.osgiliath.domain.shared.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for RecordPaymentCommand
 * Implements the critical business logic:
 * 1. Fetch and validate invoice
 * 2. Validate payment amount against balance
 * 3. Create payment domain object
 * 4. Apply payment to invoice (updates balance and may transition to PAID)
 * 5. Save both payment and invoice atomically
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordPaymentHandler {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public RecordPaymentResult handle(RecordPaymentCommand command) {
        // 1. Fetch invoice
        Invoice invoice = invoiceRepository.findById(command.getInvoiceId())
                .orElseThrow(() -> new DomainException(
                        "Invoice not found: " + command.getInvoiceId()
                ));

        // 2. Validate invoice status - must be SENT or OVERDUE
        if (invoice.getStatus() != InvoiceStatus.SENT && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new InvoiceNotSentException(
                    "Payments can only be applied to SENT or OVERDUE invoices. Current status: " + invoice.getStatus()
            );
        }

        // 3. Convert amount and validate against balance
        Money paymentAmount = Money.of(command.getAmount());

        // 4. Payment amount must not exceed balance due
        if (paymentAmount.isGreaterThan(invoice.getBalanceDue())) {
            throw new InsufficientBalanceException(
                    String.format("Payment amount %.2f exceeds invoice balance due %.2f",
                            paymentAmount.getAmount(), invoice.getBalanceDue().getAmount())
            );
        }

        // 4. Create payment domain object
        Payment payment = Payment.create(
                command.getInvoiceId(),
                command.getPaymentDate(),
                paymentAmount,
                command.getPaymentMethod(),
                command.getReferenceNumber()
        );

        // 5. Apply payment to invoice - this updates balance and may transition to PAID
        invoice.applyPayment(paymentAmount);

        // 6. Save payment and updated invoice atomically (within transaction)
        Payment savedPayment = paymentRepository.save(payment);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // 7. Return result with both payment and updated invoice info
        return new RecordPaymentResult(
                savedPayment.getId(),
                savedInvoice.getId(),
                savedPayment.getAmount(),
                savedInvoice.getBalanceDue(),
                savedInvoice.getStatus()
        );
    }
}
