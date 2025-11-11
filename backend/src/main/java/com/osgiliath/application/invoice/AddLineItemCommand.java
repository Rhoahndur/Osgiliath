package com.osgiliath.application.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Command to add a line item to an invoice (DRAFT only)
 */
@Getter
@AllArgsConstructor
public class AddLineItemCommand {
    private final UUID invoiceId;
    private final String description;
    private final String quantity;
    private final String unitPrice;
}
