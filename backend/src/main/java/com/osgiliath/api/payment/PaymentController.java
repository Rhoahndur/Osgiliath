package com.osgiliath.api.payment;

import com.osgiliath.application.payment.command.RecordPaymentCommand;
import com.osgiliath.application.payment.command.RecordPaymentHandler;
import com.osgiliath.application.payment.command.RecordPaymentResult;
import com.osgiliath.application.payment.dto.PaymentMapper;
import com.osgiliath.application.payment.dto.PaymentResponse;
import com.osgiliath.application.payment.dto.RecordPaymentRequest;
import com.osgiliath.application.payment.query.GetPaymentByIdQuery;
import com.osgiliath.application.payment.query.GetPaymentByIdQueryHandler;
import com.osgiliath.application.payment.query.ListPaymentsForInvoiceQuery;
import com.osgiliath.application.payment.query.ListPaymentsForInvoiceQueryHandler;
import com.osgiliath.domain.payment.Payment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Payment operations
 * Manages payment recording and retrieval with automatic invoice state transitions
 */
@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management APIs")
public class PaymentController {

    private final RecordPaymentHandler recordPaymentHandler;
    private final GetPaymentByIdQueryHandler getPaymentByIdQueryHandler;
    private final ListPaymentsForInvoiceQueryHandler listPaymentsForInvoiceQueryHandler;
    private final PaymentMapper paymentMapper;

    @PostMapping("/invoices/{invoiceId}/payments")
    @Operation(
            summary = "Record a payment",
            description = "Records a payment against an invoice. Validates invoice status (must be SENT), " +
                    "validates amount against balance, and automatically transitions invoice to PAID when balance reaches zero."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Payment recorded successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - payment validation failed"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Invoice not found"
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Business rule violation - invoice not in SENT status or payment exceeds balance"
            )
    })
    public ResponseEntity<PaymentResponse> recordPayment(
            @Parameter(description = "Invoice ID", required = true)
            @PathVariable UUID invoiceId,
            @Valid @RequestBody RecordPaymentRequest request
    ) {
        RecordPaymentCommand command = paymentMapper.toCommand(invoiceId, request);
        RecordPaymentResult result = recordPaymentHandler.handle(command);

        // Fetch the saved payment to get full details
        Payment payment = getPaymentByIdQueryHandler.handle(
                new GetPaymentByIdQuery(result.getPaymentId())
        );

        PaymentResponse response = paymentMapper.toResponse(result, payment);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/payments/{id}")
    @Operation(
            summary = "Get payment by ID",
            description = "Retrieves detailed information about a specific payment"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment found",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payment not found"
            )
    })
    public ResponseEntity<PaymentResponse> getPaymentById(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable UUID id
    ) {
        GetPaymentByIdQuery query = new GetPaymentByIdQuery(id);
        Payment payment = getPaymentByIdQueryHandler.handle(query);

        PaymentResponse response = paymentMapper.toResponse(payment);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/invoices/{invoiceId}/payments")
    @Operation(
            summary = "List payments for an invoice",
            description = "Retrieves all payments recorded against a specific invoice"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Payments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Invoice not found"
            )
    })
    public ResponseEntity<List<PaymentResponse>> listPaymentsForInvoice(
            @Parameter(description = "Invoice ID", required = true)
            @PathVariable UUID invoiceId
    ) {
        ListPaymentsForInvoiceQuery query = new ListPaymentsForInvoiceQuery(invoiceId);
        List<Payment> payments = listPaymentsForInvoiceQueryHandler.handle(query);

        List<PaymentResponse> responses = payments.stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
