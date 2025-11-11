package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.shared.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for CancelInvoiceCommand
 * Cancels an invoice (only DRAFT or SENT invoices can be cancelled)
 */
@Service
@RequiredArgsConstructor
public class CancelInvoiceHandler {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void handle(CancelInvoiceCommand command) {
        Invoice invoice = invoiceRepository.findById(command.getInvoiceId())
                .orElseThrow(() -> new DomainException("Invoice not found: " + command.getInvoiceId()));

        // The cancel() method in Invoice domain entity handles validation
        // It will throw DomainException if invoice cannot be cancelled (e.g., already PAID)
        invoice.cancel();
        invoiceRepository.save(invoice);

        // Note: The reason field from command could be logged or stored in an audit trail
        // For now, it's available in the command but not persisted
    }
}
