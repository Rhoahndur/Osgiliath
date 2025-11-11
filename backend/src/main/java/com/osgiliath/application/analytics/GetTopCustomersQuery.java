package com.osgiliath.application.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query to get top customers by revenue
 * Returns top N customers ranked by total revenue from paid invoices
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetTopCustomersQuery {
    private Integer limit = 10; // Default to top 10 customers
}
