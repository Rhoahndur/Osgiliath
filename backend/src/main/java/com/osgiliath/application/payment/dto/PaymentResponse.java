package com.osgiliath.application.payment.dto;

import com.osgiliath.domain.invoice.InvoiceStatus;
import com.osgiliath.domain.payment.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for payment operations
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payment details")
public class PaymentResponse {

    @Schema(description = "Payment ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Invoice ID", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID invoiceId;

    @Schema(description = "Payment amount", example = "500.00")
    private BigDecimal amount;

    @Schema(description = "Date the payment was received", example = "2024-01-15")
    private LocalDate paymentDate;

    @Schema(description = "Method of payment", example = "BANK_TRANSFER")
    private PaymentMethod paymentMethod;

    @Schema(description = "Payment reference or transaction number", example = "TXN-12345")
    private String referenceNumber;

    @Schema(description = "Timestamp when payment was recorded", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Updated invoice balance after payment", example = "500.00")
    private BigDecimal updatedInvoiceBalance;

    @Schema(description = "Updated invoice status after payment", example = "SENT")
    private InvoiceStatus updatedInvoiceStatus;
}
