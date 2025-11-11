package com.osgiliath.application.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for line item
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Line item details")
public class LineItemRequest {

    @NotBlank(message = "Description is required")
    @Schema(description = "Line item description", example = "Web Development Services")
    private String description;

    @NotBlank(message = "Quantity is required")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Quantity must be a valid number")
    @Schema(description = "Quantity", example = "10.5")
    private String quantity;

    @NotBlank(message = "Unit price is required")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Unit price must be a valid number")
    @Schema(description = "Unit price", example = "100.00")
    private String unitPrice;
}
