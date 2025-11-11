package com.osgiliath.application.analytics;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query to get invoice status breakdown
 * Returns count of invoices by status
 */
@Data
@NoArgsConstructor
public class GetInvoiceStatusBreakdownQuery {
    // No parameters needed - returns all statuses
}
