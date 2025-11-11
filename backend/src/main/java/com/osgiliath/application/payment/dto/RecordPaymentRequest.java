package com.osgiliath.application.payment.dto;

import com.osgiliath.domain.payment.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for recording a payment
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request to record a payment against an invoice")
public class RecordPaymentRequest {

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Payment amount must have at most 2 decimal places")
    @Schema(description = "Payment amount", example = "500.00", required = true)
    private BigDecimal amount;

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    @Schema(description = "Date the payment was received", example = "2024-01-15", required = true)
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    @Schema(description = "Method of payment", example = "BANK_TRANSFER", required = true)
    private PaymentMethod paymentMethod;

    @Size(max = 100, message = "Reference number cannot exceed 100 characters")
    @Schema(description = "Payment reference or transaction number", example = "TXN-12345", required = false)
    private String referenceNumber;
}
