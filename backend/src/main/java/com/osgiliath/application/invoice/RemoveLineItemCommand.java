package com.osgiliath.application.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Command to remove a line item from an invoice (DRAFT only)
 */
@AllArgsConstructor
@Getter
public class RemoveLineItemCommand {
    private final UUID invoiceId;
    private final UUID lineItemId;
}
