package com.osgiliath.application.payment.query;

import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.payment.Payment;
import com.osgiliath.domain.payment.PaymentRepository;
import com.osgiliath.domain.shared.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handler for ListPaymentsForInvoiceQuery
 * Validates invoice exists before returning payments
 */
@Service
@RequiredArgsConstructor
public class ListPaymentsForInvoiceQueryHandler {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public List<Payment> handle(ListPaymentsForInvoiceQuery query) {
        // Validate invoice exists
        invoiceRepository.findById(query.getInvoiceId())
                .orElseThrow(() -> new DomainException(
                        "Invoice not found: " + query.getInvoiceId()
                ));

        return paymentRepository.findByInvoiceId(query.getInvoiceId());
    }
}
