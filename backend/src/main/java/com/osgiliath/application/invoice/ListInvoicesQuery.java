package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Query to list invoices with optional filters
 * Supports filtering by status, customer, and date range with sorting
 */
@AllArgsConstructor
@Getter
public class ListInvoicesQuery {
    private final InvoiceStatus status;
    private final UUID customerId;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final Integer page;
    private final Integer size;
    private final String sortBy;
    private final String sortDirection;
}
