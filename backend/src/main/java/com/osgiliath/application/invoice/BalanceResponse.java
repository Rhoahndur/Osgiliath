package com.osgiliath.application.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for invoice balance information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Invoice balance information")
public class BalanceResponse {

    @Schema(description = "Total amount", example = "1100.00")
    private BigDecimal totalAmount;

    @Schema(description = "Balance due", example = "550.00")
    private BigDecimal balanceDue;

    @Schema(description = "Amount paid", example = "550.00")
    private BigDecimal amountPaid;
}
