package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.shared.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for RemoveLineItemCommand
 * Removes a line item from an invoice (DRAFT status only)
 */
@Service
@RequiredArgsConstructor
public class RemoveLineItemHandler {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void handle(RemoveLineItemCommand command) {
        Invoice invoice = invoiceRepository.findById(command.getInvoiceId())
                .orElseThrow(() -> new DomainException("Invoice not found: " + command.getInvoiceId()));

        invoice.removeLineItem(command.getLineItemId());
        invoiceRepository.save(invoice);
    }
}
