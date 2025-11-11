package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.invoice.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkOverdueInvoicesHandler {
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public int handle(MarkOverdueInvoicesCommand command) {
        LocalDate today = LocalDate.now();

        // Find all SENT invoices where due date is past using optimized database query
        List<Invoice> overdueInvoices = invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.SENT, today);

        for (Invoice invoice : overdueInvoices) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
        }

        log.info("Marked {} invoices as OVERDUE", overdueInvoices.size());
        return overdueInvoices.size();
    }
}
