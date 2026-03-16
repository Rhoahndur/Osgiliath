package com.osgiliath.application.invoice;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Query to get an invoice's balance information */
@Getter
@AllArgsConstructor
public class GetInvoiceBalanceQuery {
    private final UUID invoiceId;
}
