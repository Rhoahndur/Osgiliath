package com.osgiliath.application.invoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.osgiliath.domain.invoice.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for invoice
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Invoice response")
public class InvoiceResponse {

    @Schema(description = "Invoice ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Customer ID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID customerId;

    @Schema(description = "Customer name", example = "John Doe")
    private String customerName;

    @Schema(description = "Invoice number", example = "INV-20251107-00001")
    private String invoiceNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Invoice issue date", example = "2025-11-07")
    private LocalDate issueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Invoice due date", example = "2025-12-07")
    private LocalDate dueDate;

    @Schema(description = "Invoice status", example = "DRAFT")
    private InvoiceStatus status;

    @Schema(description = "List of line items")
    private List<LineItemResponse> lineItems;

    @Schema(description = "Subtotal amount", example = "1000.00")
    private BigDecimal subtotal;

    @Schema(description = "Tax amount", example = "100.00")
    private BigDecimal taxAmount;

    @Schema(description = "Total amount", example = "1100.00")
    private BigDecimal totalAmount;

    @Schema(description = "Balance due", example = "1100.00")
    private BigDecimal balanceDue;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
