package com.osgiliath.application.payment.query;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Query to retrieve all payments for a specific invoice */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ListPaymentsForInvoiceQuery {

    private UUID invoiceId;
}
