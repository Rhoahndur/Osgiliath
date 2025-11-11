package com.osgiliath.application.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query to get revenue over time
 * Returns monthly revenue for the last 12 months
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetRevenueOverTimeQuery {
    private Integer months = 12; // Default to last 12 months
}
