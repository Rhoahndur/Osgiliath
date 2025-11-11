package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.shared.DomainException;
import com.osgiliath.domain.shared.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Handler for AddLineItemCommand
 * Adds a line item to an invoice (DRAFT status only)
 */
@Service
@RequiredArgsConstructor
public class AddLineItemHandler {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public UUID handle(AddLineItemCommand command) {
        Invoice invoice = invoiceRepository.findById(command.getInvoiceId())
                .orElseThrow(() -> new DomainException("Invoice not found: " + command.getInvoiceId()));

        invoice.addLineItem(
                command.getDescription(),
                new BigDecimal(command.getQuantity()),
                Money.of(new BigDecimal(command.getUnitPrice()))
        );

        Invoice saved = invoiceRepository.save(invoice);
        // Return the ID of the newly added line item (last one in the list)
        return saved.getLineItems().get(saved.getLineItems().size() - 1).getId();
    }
}
