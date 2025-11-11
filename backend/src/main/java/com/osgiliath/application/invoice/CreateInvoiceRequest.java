package com.osgiliath.application.invoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating an invoice
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new invoice")
public class CreateInvoiceRequest {

    @NotNull(message = "Customer ID is required")
    @Schema(description = "ID of the customer", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID customerId;

    @NotNull(message = "Issue date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Invoice issue date", example = "2025-11-07")
    private LocalDate issueDate;

    @NotNull(message = "Due date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Invoice due date", example = "2025-12-07")
    private LocalDate dueDate;

    @Valid
    @Schema(description = "List of line items")
    private List<LineItemRequest> lineItems;
}
