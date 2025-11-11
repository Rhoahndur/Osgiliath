package com.osgiliath.application.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for line item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Line item response")
public class LineItemResponse {

    @Schema(description = "Line item ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Line item description", example = "Web Development Services")
    private String description;

    @Schema(description = "Quantity", example = "10.5")
    private BigDecimal quantity;

    @Schema(description = "Unit price", example = "100.00")
    private BigDecimal unitPrice;

    @Schema(description = "Line total", example = "1050.00")
    private BigDecimal lineTotal;
}
