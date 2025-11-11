package com.osgiliath.domain.invoice;

import com.osgiliath.domain.exceptions.InvoiceHasNoLineItemsException;
import com.osgiliath.domain.shared.BaseEntity;
import com.osgiliath.domain.shared.DomainException;
import com.osgiliath.domain.shared.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Invoice Aggregate Root
 * Manages invoice lifecycle and enforces business rules
 */
@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoice_number", columnList = "invoice_number", unique = true),
        @Index(name = "idx_invoice_customer", columnList = "customer_id"),
        @Index(name = "idx_invoice_status", columnList = "status"),
        @Index(name = "idx_invoice_issue_date", columnList = "issue_date")
})
@Getter
@NoArgsConstructor
public class Invoice extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineItem> lineItems = new ArrayList<>();

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "subtotal", nullable = false))
    private Money subtotal;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "tax_amount", nullable = false))
    private Money taxAmount;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_amount", nullable = false))
    private Money totalAmount;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "balance_due", nullable = false))
    private Money balanceDue;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.10"); // 10% tax

    private Invoice(UUID customerId, String invoiceNumber, LocalDate issueDate, LocalDate dueDate) {
        this.customerId = customerId;
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.status = InvoiceStatus.DRAFT;
        this.subtotal = Money.zero();
        this.taxAmount = Money.zero();
        this.totalAmount = Money.zero();
        this.balanceDue = Money.zero();
    }

    /**
     * Factory method to create a new invoice
     */
    public static Invoice create(UUID customerId, String invoiceNumber, LocalDate issueDate, LocalDate dueDate) {
        validateCustomerId(customerId);
        validateInvoiceNumber(invoiceNumber);
        validateDates(issueDate, dueDate);

        return new Invoice(customerId, invoiceNumber, issueDate, dueDate);
    }

    /**
     * Add a line item to the invoice (only in DRAFT status)
     */
    public void addLineItem(String description, BigDecimal quantity, Money unitPrice) {
        ensureDraftStatus("Cannot add line items to a non-draft invoice");

        LineItem lineItem = new LineItem(this, description, quantity, unitPrice);
        lineItems.add(lineItem);
        recalculateTotals();
    }

    /**
     * Remove a line item from the invoice (only in DRAFT status)
     */
    public void removeLineItem(UUID lineItemId) {
        ensureDraftStatus("Cannot remove line items from a non-draft invoice");

        boolean removed = lineItems.removeIf(item -> item.getId().equals(lineItemId));
        if (!removed) {
            throw new DomainException("Line item not found: " + lineItemId);
        }
        recalculateTotals();
    }

    /**
     * Update invoice details (only in DRAFT status)
     */
    public void update(LocalDate issueDate, LocalDate dueDate) {
        ensureDraftStatus("Cannot update a non-draft invoice");
        validateDates(issueDate, dueDate);

        this.issueDate = issueDate;
        this.dueDate = dueDate;
    }

    /**
     * Send the invoice (transition from DRAFT to SENT)
     */
    public void send() {
        if (status != InvoiceStatus.DRAFT) {
            throw new DomainException("Only draft invoices can be sent");
        }
        if (lineItems.isEmpty()) {
            throw new InvoiceHasNoLineItemsException("Cannot send an invoice without line items");
        }

        this.status = InvoiceStatus.SENT;
        this.balanceDue = this.totalAmount;
    }

    /**
     * Apply a payment to the invoice
     */
    public void applyPayment(Money paymentAmount) {
        if (status != InvoiceStatus.SENT && status != InvoiceStatus.OVERDUE) {
            throw new DomainException("Can only apply payments to SENT or OVERDUE invoices. Current status: " + status);
        }
        if (paymentAmount.isNegative() || paymentAmount.isZero()) {
            throw new DomainException("Payment amount must be greater than zero");
        }
        if (paymentAmount.isGreaterThan(balanceDue)) {
            throw new DomainException("Payment amount cannot exceed balance due");
        }

        this.balanceDue = this.balanceDue.subtract(paymentAmount);

        // Auto-transition to PAID when balance is zero
        if (this.balanceDue.isZero()) {
            this.status = InvoiceStatus.PAID;
        }
    }

    /**
     * Manually mark invoice as paid (administrative override)
     */
    public void markAsPaid() {
        if (status != InvoiceStatus.SENT && status != InvoiceStatus.OVERDUE) {
            throw new DomainException("Can only mark SENT or OVERDUE invoices as paid. Current status: " + status);
        }
        this.balanceDue = Money.zero();
        this.status = InvoiceStatus.PAID;
    }

    /**
     * Cancel the invoice (can be done from DRAFT or SENT status)
     */
    public void cancel() {
        if (status != InvoiceStatus.DRAFT && status != InvoiceStatus.SENT) {
            throw new DomainException("Can only cancel draft or sent invoices");
        }

        this.status = InvoiceStatus.CANCELLED;
        this.balanceDue = Money.zero();
    }

    /**
     * Set status directly (for system operations like scheduled tasks)
     */
    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    /**
     * Recalculate all totals based on line items
     */
    private void recalculateTotals() {
        this.subtotal = lineItems.stream()
                .map(LineItem::getLineTotal)
                .reduce(Money.zero(), Money::add);

        this.taxAmount = subtotal.multiply(TAX_RATE);
        this.totalAmount = subtotal.add(taxAmount);

        // Only update balance if invoice hasn't been sent yet
        if (status == InvoiceStatus.DRAFT) {
            this.balanceDue = Money.zero();
        }
    }

    private void ensureDraftStatus(String message) {
        if (status != InvoiceStatus.DRAFT) {
            throw new DomainException(message);
        }
    }

    private static void validateCustomerId(UUID customerId) {
        if (customerId == null) {
            throw new DomainException("Customer ID cannot be null");
        }
    }

    private static void validateInvoiceNumber(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new DomainException("Invoice number cannot be empty");
        }
        if (invoiceNumber.length() > 50) {
            throw new DomainException("Invoice number cannot exceed 50 characters");
        }
    }

    private static void validateDates(LocalDate issueDate, LocalDate dueDate) {
        if (issueDate == null) {
            throw new DomainException("Issue date cannot be null");
        }
        if (dueDate == null) {
            throw new DomainException("Due date cannot be null");
        }
        if (dueDate.isBefore(issueDate)) {
            throw new DomainException("Due date cannot be before issue date");
        }
    }
}
