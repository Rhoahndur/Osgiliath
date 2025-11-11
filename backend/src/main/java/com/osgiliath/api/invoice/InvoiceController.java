package com.osgiliath.api.invoice;

import com.osgiliath.application.invoice.*;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for Invoice Management
 * Provides CRUD operations with CQRS pattern
 */
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoice Management", description = "APIs for managing invoices with state machine lifecycle")
public class InvoiceController {

    private final CreateInvoiceHandler createInvoiceHandler;
    private final UpdateInvoiceHandler updateInvoiceHandler;
    private final AddLineItemHandler addLineItemHandler;
    private final RemoveLineItemHandler removeLineItemHandler;
    private final SendInvoiceHandler sendInvoiceHandler;
    private final MarkInvoiceAsPaidHandler markInvoiceAsPaidHandler;
    private final CancelInvoiceHandler cancelInvoiceHandler;
    private final DeleteInvoiceHandler deleteInvoiceHandler;
    private final GetInvoiceByIdQueryHandler getInvoiceByIdQueryHandler;
    private final ListInvoicesQueryHandler listInvoicesQueryHandler;
    private final GetInvoiceBalanceQueryHandler getInvoiceBalanceQueryHandler;
    private final ExportInvoiceToPdfQueryHandler exportInvoiceToPdfQueryHandler;
    private final InvoiceMapper invoiceMapper;

    @PostMapping
    @Operation(summary = "Create a new invoice", description = "Creates a new invoice with line items in DRAFT status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invoice created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        CreateInvoiceCommand command = invoiceMapper.toCommand(request);
        UUID invoiceId = createInvoiceHandler.handle(command);

        Invoice invoice = getInvoiceByIdQueryHandler.handle(new GetInvoiceByIdQuery(invoiceId));
        InvoiceResponse response = invoiceMapper.toResponse(invoice);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID", description = "Retrieves an invoice with all line items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice found"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @Parameter(description = "Invoice ID") @PathVariable UUID id) {
        Invoice invoice = getInvoiceByIdQueryHandler.handle(new GetInvoiceByIdQuery(id));
        InvoiceResponse response = invoiceMapper.toResponse(invoice);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List invoices", description = "Lists invoices with optional filters, pagination and sorting")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of invoices")
    })
    public ResponseEntity<List<InvoiceResponse>> listInvoices(
            @Parameter(description = "Filter by status") @RequestParam(required = false) InvoiceStatus status,
            @Parameter(description = "Filter by customer ID") @RequestParam(required = false) UUID customerId,
            @Parameter(description = "Filter by issue date from") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Filter by issue date to") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Page number (0-based)") @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Page size") @RequestParam(required = false, defaultValue = "20") Integer size,
            @Parameter(description = "Sort field") @RequestParam(required = false, defaultValue = "issueDate") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)") @RequestParam(required = false, defaultValue = "DESC") String sortDirection) {

        ListInvoicesQuery query = new ListInvoicesQuery(status, customerId, fromDate, toDate, page, size, sortBy, sortDirection);
        List<Invoice> invoices = listInvoicesQueryHandler.handle(query);

        List<InvoiceResponse> responses = invoices.stream()
                .map(invoiceMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update invoice", description = "Updates an invoice (DRAFT status only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or invoice not in DRAFT status"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @Parameter(description = "Invoice ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request) {

        UpdateInvoiceCommand command = invoiceMapper.toUpdateCommand(id, request);
        updateInvoiceHandler.handle(command);

        Invoice invoice = getInvoiceByIdQueryHandler.handle(new GetInvoiceByIdQuery(id));
        InvoiceResponse response = invoiceMapper.toResponse(invoice);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/line-items")
    @Operation(summary = "Add line item", description = "Adds a line item to an invoice (DRAFT status only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Line item added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or invoice not in DRAFT status"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<LineItemResponse> addLineItem(
            @Parameter(description = "Invoice ID") @PathVariable UUID id,
            @Valid @RequestBody LineItemRequest request) {

        AddLineItemCommand command = invoiceMapper.toAddLineItemCommand(id, request);
        UUID lineItemId = addLineItemHandler.handle(command);

        Invoice invoice = getInvoiceByIdQueryHandler.handle(new GetInvoiceByIdQuery(id));
        LineItemResponse response = invoice.getLineItems().stream()
                .filter(item -> item.getId().equals(lineItemId))
                .map(invoiceMapper::toLineItemResponse)
                .findFirst()
                .orElseThrow();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/line-items/{lineItemId}")
    @Operation(summary = "Remove line item", description = "Removes a line item from an invoice (DRAFT status only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Line item removed successfully"),
            @ApiResponse(responseCode = "400", description = "Invoice not in DRAFT status"),
            @ApiResponse(responseCode = "404", description = "Invoice or line item not found")
    })
    public ResponseEntity<Void> removeLineItem(
            @Parameter(description = "Invoice ID") @PathVariable UUID id,
            @Parameter(description = "Line item ID") @PathVariable UUID lineItemId) {

        RemoveLineItemCommand command = new RemoveLineItemCommand(id, lineItemId);
        removeLineItemHandler.handle(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Send invoice", description = "Transitions invoice from DRAFT to SENT status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invoice not in DRAFT status or has no line items"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceResponse> sendInvoice(
            @Parameter(description = "Invoice ID") @PathVariable UUID id) {

        SendInvoiceCommand command = new SendInvoiceCommand(id);
        sendInvoiceHandler.handle(command);

        Invoice invoice = getInvoiceByIdQueryHandler.handle(new GetInvoiceByIdQuery(id));
        InvoiceResponse response = invoiceMapper.toResponse(invoice);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/mark-paid")
    @Operation(summary = "Mark invoice as paid", description = "Manually marks a SENT invoice as PAID (administrative override)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice marked as paid successfully"),
            @ApiResponse(responseCode = "400", description = "Invoice not in SENT status"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceResponse> markInvoiceAsPaid(
            @Parameter(description = "Invoice ID") @PathVariable UUID id) {

        MarkInvoiceAsPaidCommand command = new MarkInvoiceAsPaidCommand(id);
        markInvoiceAsPaidHandler.handle(command);

        Invoice invoice = getInvoiceByIdQueryHandler.handle(new GetInvoiceByIdQuery(id));
        InvoiceResponse response = invoiceMapper.toResponse(invoice);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel invoice", description = "Cancel a draft or sent invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invoice cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Invoice cannot be cancelled (already PAID or CANCELLED)"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<Void> cancelInvoice(
            @Parameter(description = "Invoice ID") @PathVariable UUID id,
            @RequestBody(required = false) CancelInvoiceRequest request) {

        String reason = request != null ? request.getReason() : null;
        CancelInvoiceCommand command = new CancelInvoiceCommand(id, reason);
        cancelInvoiceHandler.handle(command);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete invoice", description = "Deletes an invoice (DRAFT status only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Invoice deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invoice not in DRAFT status"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<Void> deleteInvoice(
            @Parameter(description = "Invoice ID") @PathVariable UUID id) {

        DeleteInvoiceCommand command = new DeleteInvoiceCommand(id);
        deleteInvoiceHandler.handle(command);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get invoice balance", description = "Retrieve current balance information for an invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance information retrieved"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceBalanceResponse> getInvoiceBalance(
            @Parameter(description = "Invoice ID") @PathVariable UUID id) {

        GetInvoiceBalanceQuery query = new GetInvoiceBalanceQuery(id);
        InvoiceBalanceResponse response = getInvoiceBalanceQueryHandler.handle(query);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Export invoice to PDF", description = "Generates and downloads a PDF document for the invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "PDF generated successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice or customer not found")
    })
    public ResponseEntity<byte[]> exportInvoiceToPdf(
            @Parameter(description = "Invoice ID") @PathVariable UUID id) {

        ExportInvoiceToPdfQuery query = new ExportInvoiceToPdfQuery(id);
        byte[] pdfBytes = exportInvoiceToPdfQueryHandler.handle(query);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"invoice-" + id + ".pdf\"")
                .body(pdfBytes);
    }
}
