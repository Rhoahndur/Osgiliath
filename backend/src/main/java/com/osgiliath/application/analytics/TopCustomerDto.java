package com.osgiliath.application.analytics;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for top customer data */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopCustomerDto {
    private UUID customerId;
    private String customerName;
    private BigDecimal totalRevenue;
    private Long invoiceCount;
}
