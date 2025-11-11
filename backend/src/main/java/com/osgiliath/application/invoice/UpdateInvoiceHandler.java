package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.shared.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for UpdateInvoiceCommand
 * Updates invoice details (DRAFT status only)
 */
@Service
@RequiredArgsConstructor
public class UpdateInvoiceHandler {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void handle(UpdateInvoiceCommand command) {
        Invoice invoice = invoiceRepository.findById(command.getInvoiceId())
                .orElseThrow(() -> new DomainException("Invoice not found: " + command.getInvoiceId()));

        invoice.update(command.getIssueDate(), command.getDueDate());
        invoiceRepository.save(invoice);
    }
}
