package com.osgiliath.application.invoice;

import com.osgiliath.domain.customer.CustomerRepository;
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
 * Handler for CreateInvoiceCommand
 * Creates a new invoice with line items
 */
@Service
@RequiredArgsConstructor
public class CreateInvoiceHandler {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;

    @Transactional
    public UUID handle(CreateInvoiceCommand command) {
        // Validate customer exists
        if (!customerRepository.findById(command.getCustomerId()).isPresent()) {
            throw new DomainException("Customer not found: " + command.getCustomerId());
        }

        // Generate invoice number
        String invoiceNumber = invoiceNumberGenerator.generate(command.getIssueDate());

        // Create invoice
        Invoice invoice = Invoice.create(
                command.getCustomerId(),
                invoiceNumber,
                command.getIssueDate(),
                command.getDueDate()
        );

        // Add line items
        if (command.getLineItems() != null && !command.getLineItems().isEmpty()) {
            for (CreateInvoiceCommand.LineItemDto lineItemDto : command.getLineItems()) {
                invoice.addLineItem(
                        lineItemDto.getDescription(),
                        new BigDecimal(lineItemDto.getQuantity()),
                        Money.of(new BigDecimal(lineItemDto.getUnitPrice()))
                );
            }
        }

        // Save and return
        Invoice saved = invoiceRepository.save(invoice);
        return saved.getId();
    }
}
