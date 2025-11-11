package com.osgiliath.api.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for cancelling an invoice
 */
@Getter
@Setter
@Schema(description = "Request to cancel an invoice")
public class CancelInvoiceRequest {

    @Schema(description = "Optional reason for cancelling the invoice", example = "Customer requested cancellation")
    private String reason;
}
