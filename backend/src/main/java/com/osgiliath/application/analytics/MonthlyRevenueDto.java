package com.osgiliath.application.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for monthly revenue data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenueDto {
    private String month; // Format: "2024-01"
    private BigDecimal revenue;

}
