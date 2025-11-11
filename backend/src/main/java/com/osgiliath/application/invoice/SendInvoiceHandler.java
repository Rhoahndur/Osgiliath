package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.shared.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for SendInvoiceCommand
 * Transitions invoice from DRAFT to SENT
 */
@Service
@RequiredArgsConstructor
public class SendInvoiceHandler {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void handle(SendInvoiceCommand command) {
        Invoice invoice = invoiceRepository.findById(command.getInvoiceId())
                .orElseThrow(() -> new DomainException("Invoice not found: " + command.getInvoiceId()));

        invoice.send();
        invoiceRepository.save(invoice);
    }
}
