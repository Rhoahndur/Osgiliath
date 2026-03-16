package com.osgiliath.application.invoice;

import com.osgiliath.domain.customer.CustomerRepository;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.shared.DomainException;
import com.osgiliath.infrastructure.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Handler for SendInvoiceCommand
 * Transitions invoice from DRAFT to SENT
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SendInvoiceHandler {

    private final InvoiceRepository invoiceRepository;
    private final Optional<EmailService> emailService;
    private final ExportInvoiceToPdfQueryHandler pdfExporter;
    private final CustomerRepository customerRepository;

    @Transactional
    public void handle(SendInvoiceCommand command) {
        Invoice invoice = invoiceRepository.findById(command.getInvoiceId())
                .orElseThrow(() -> new DomainException("Invoice not found: " + command.getInvoiceId()));

        invoice.send();
        invoiceRepository.save(invoice);

        // Send email notification if email service is enabled
        emailService.ifPresent(service -> {
            try {
                customerRepository.findById(invoice.getCustomerId()).ifPresent(customer -> {
                    byte[] pdfBytes = pdfExporter.handle(
                        new ExportInvoiceToPdfQuery(invoice.getId()));
                    service.sendInvoiceEmail(invoice, customer, pdfBytes);
                });
            } catch (Exception e) {
                // Log but don't fail the invoice send
                log.warn("Failed to send invoice email for {}: {}",
                    invoice.getInvoiceNumber(), e.getMessage());
            }
        });
    }
}
