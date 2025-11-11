package com.osgiliath.application.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Query to get an invoice by ID with line items and customer data
 */
@AllArgsConstructor
@Getter
public class GetInvoiceByIdQuery {
    private final UUID invoiceId;
}
