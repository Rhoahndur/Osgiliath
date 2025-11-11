package com.osgiliath.application.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Command to create a new invoice with line items
 */
@Getter
@AllArgsConstructor
public class CreateInvoiceCommand {
    private final UUID customerId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final List<LineItemDto> lineItems;

    @Getter
    @AllArgsConstructor
    public static class LineItemDto {
        private final String description;
        private final String quantity;
        private final String unitPrice;
    }
}
