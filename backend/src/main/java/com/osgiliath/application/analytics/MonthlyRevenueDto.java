package com.osgiliath.application.analytics;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for monthly revenue data */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenueDto {
    private String month; // Format: "2024-01"
    private BigDecimal revenue;
}
