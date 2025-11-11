package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.shared.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for GetInvoiceBalanceQuery
 * Returns balance information for an invoice
 */
@Service
@RequiredArgsConstructor
public class GetInvoiceBalanceQueryHandler {

    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public InvoiceBalanceResponse handle(GetInvoiceBalanceQuery query) {
        Invoice invoice = invoiceRepository.findById(query.getInvoiceId())
                .orElseThrow(() -> new DomainException("Invoice not found: " + query.getInvoiceId()));

        return InvoiceBalanceResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .totalAmount(invoice.getTotalAmount().getAmount())
                .paidAmount(invoice.getTotalAmount().subtract(invoice.getBalanceDue()).getAmount())
                .balanceDue(invoice.getBalanceDue().getAmount())
                .build();
    }
}
