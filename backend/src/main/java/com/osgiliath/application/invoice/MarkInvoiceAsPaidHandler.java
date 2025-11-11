package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.shared.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for MarkInvoiceAsPaidCommand
 * Manually marks invoice as paid (administrative override)
 */
@Service
@RequiredArgsConstructor
public class MarkInvoiceAsPaidHandler {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void handle(MarkInvoiceAsPaidCommand command) {
        Invoice invoice = invoiceRepository.findById(command.getInvoiceId())
                .orElseThrow(() -> new DomainException("Invoice not found: " + command.getInvoiceId()));

        invoice.markAsPaid();
        invoiceRepository.save(invoice);
    }
}
