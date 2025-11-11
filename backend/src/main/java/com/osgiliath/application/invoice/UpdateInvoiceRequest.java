package com.osgiliath.application.invoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for updating an invoice (DRAFT only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an invoice (DRAFT status only)")
public class UpdateInvoiceRequest {

    @NotNull(message = "Issue date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Invoice issue date", example = "2025-11-07")
    private LocalDate issueDate;

    @NotNull(message = "Due date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Invoice due date", example = "2025-12-07")
    private LocalDate dueDate;
}
