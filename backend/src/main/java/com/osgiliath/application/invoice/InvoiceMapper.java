package com.osgiliath.application.invoice;

import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.customer.CustomerRepository;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.LineItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper to convert between Invoice domain entities and DTOs
 */
@Component
@RequiredArgsConstructor
public class InvoiceMapper {

    private final CustomerRepository customerRepository;

    /**
     * Map CreateInvoiceRequest to CreateInvoiceCommand
     */
    public CreateInvoiceCommand toCommand(CreateInvoiceRequest request) {
        List<CreateInvoiceCommand.LineItemDto> lineItemDtos = null;

        if (request.getLineItems() != null) {
            lineItemDtos = request.getLineItems().stream()
                    .map(item -> new CreateInvoiceCommand.LineItemDto(
                            item.getDescription(),
                            item.getQuantity(),
                            item.getUnitPrice()
                    ))
                    .collect(Collectors.toList());
        }

        return new CreateInvoiceCommand(
                request.getCustomerId(),
                request.getIssueDate(),
                request.getDueDate(),
                lineItemDtos
        );
    }

    /**
     * Map LineItemRequest to AddLineItemCommand
     */
    public AddLineItemCommand toAddLineItemCommand(java.util.UUID invoiceId, LineItemRequest request) {
        return new AddLineItemCommand(
                invoiceId,
                request.getDescription(),
                request.getQuantity(),
                request.getUnitPrice()
        );
    }

    /**
     * Map UpdateInvoiceRequest to UpdateInvoiceCommand
     */
    public UpdateInvoiceCommand toUpdateCommand(java.util.UUID invoiceId, UpdateInvoiceRequest request) {
        return new UpdateInvoiceCommand(
                invoiceId,
                request.getIssueDate(),
                request.getDueDate()
        );
    }

    /**
     * Map Invoice entity to InvoiceResponse DTO
     */
    public InvoiceResponse toResponse(Invoice invoice) {
        // Look up customer name
        String customerName = customerRepository.findById(invoice.getCustomerId())
                .map(Customer::getName)
                .orElse("Unknown");

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .customerId(invoice.getCustomerId())
                .customerName(customerName)
                .invoiceNumber(invoice.getInvoiceNumber())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .lineItems(toLineItemResponses(invoice.getLineItems()))
                .subtotal(invoice.getSubtotal().getAmount())
                .taxAmount(invoice.getTaxAmount().getAmount())
                .totalAmount(invoice.getTotalAmount().getAmount())
                .balanceDue(invoice.getBalanceDue().getAmount())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    /**
     * Map LineItem entity to LineItemResponse DTO
     */
    public LineItemResponse toLineItemResponse(LineItem lineItem) {
        return LineItemResponse.builder()
                .id(lineItem.getId())
                .description(lineItem.getDescription())
                .quantity(lineItem.getQuantity())
                .unitPrice(lineItem.getUnitPrice().getAmount())
                .lineTotal(lineItem.getLineTotal().getAmount())
                .build();
    }

    /**
     * Map list of LineItem entities to list of LineItemResponse DTOs
     */
    public List<LineItemResponse> toLineItemResponses(List<LineItem> lineItems) {
        if (lineItems == null) {
            return List.of();
        }
        return lineItems.stream()
                .map(this::toLineItemResponse)
                .collect(Collectors.toList());
    }
}
