package com.osgiliath.application.invoice;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Command to remove a line item from an invoice (DRAFT only) */
@AllArgsConstructor
@Getter
public class RemoveLineItemCommand {
    private final UUID invoiceId;
    private final UUID lineItemId;
}
