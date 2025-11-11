package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.shared.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for GetInvoiceByIdQuery
 * Retrieves an invoice with eager-loaded line items
 */
@Service
@RequiredArgsConstructor
public class GetInvoiceByIdQueryHandler {

    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public Invoice handle(GetInvoiceByIdQuery query) {
        return invoiceRepository.findById(query.getInvoiceId())
                .orElseThrow(() -> new DomainException("Invoice not found: " + query.getInvoiceId()));
    }
}
