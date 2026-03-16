package com.osgiliath.application.invoice;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Query to get an invoice by ID with line items and customer data */
@AllArgsConstructor
@Getter
public class GetInvoiceByIdQuery {
    private final UUID invoiceId;
}
