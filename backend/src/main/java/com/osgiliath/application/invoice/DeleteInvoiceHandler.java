package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.invoice.InvoiceStatus;
import com.osgiliath.domain.shared.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for DeleteInvoiceCommand
 * Deletes an invoice (DRAFT status only)
 */
@Service
@RequiredArgsConstructor
public class DeleteInvoiceHandler {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void handle(DeleteInvoiceCommand command) {
        Invoice invoice = invoiceRepository.findById(command.getInvoiceId())
                .orElseThrow(() -> new DomainException("Invoice not found: " + command.getInvoiceId()));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new DomainException("Can only delete draft invoices");
        }

        invoiceRepository.delete(invoice);
    }
}
