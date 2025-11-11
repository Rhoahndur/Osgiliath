package com.osgiliath.application.payment.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Query to retrieve all payments for a specific invoice
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ListPaymentsForInvoiceQuery {

    private UUID invoiceId;
}
