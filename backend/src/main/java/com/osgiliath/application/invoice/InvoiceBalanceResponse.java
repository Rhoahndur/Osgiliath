package com.osgiliath.application.invoice;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvoiceBalanceResponse {
    private UUID invoiceId;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
}
