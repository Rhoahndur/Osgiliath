package com.osgiliath.domain.invoice;

import com.osgiliath.domain.shared.DomainException;
import com.osgiliath.domain.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Invoice aggregate
 * Tests invoice lifecycle, business rules, and state transitions
 */
@DisplayName("Invoice Aggregate")
class InvoiceTest {

    private UUID customerId;
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate dueDate;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        invoiceNumber = "INV-001";
        issueDate = LocalDate.now();
        dueDate = issueDate.plusDays(30);
    }

    @Test
    @DisplayName("Should create invoice with valid data")
    void shouldCreateInvoiceWithValidData() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);

        assertThat(invoice.getCustomerId()).isEqualTo(customerId);
        assertThat(invoice.getInvoiceNumber()).isEqualTo(invoiceNumber);
        assertThat(invoice.getIssueDate()).isEqualTo(issueDate);
        assertThat(invoice.getDueDate()).isEqualTo(dueDate);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.getSubtotal()).isEqualTo(Money.zero());
        assertThat(invoice.getTotalAmount()).isEqualTo(Money.zero());
        assertThat(invoice.getBalanceDue()).isEqualTo(Money.zero());
    }

    @Test
    @DisplayName("Should fail when customer ID is null")
    void shouldFailWhenCustomerIdIsNull() {
        assertThatThrownBy(() -> Invoice.create(null, invoiceNumber, issueDate, dueDate))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Customer ID cannot be null");
    }

    @Test
    @DisplayName("Should fail when invoice number is null")
    void shouldFailWhenInvoiceNumberIsNull() {
        assertThatThrownBy(() -> Invoice.create(customerId, null, issueDate, dueDate))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Invoice number cannot be empty");
    }

    @Test
    @DisplayName("Should fail when invoice number is blank")
    void shouldFailWhenInvoiceNumberIsBlank() {
        assertThatThrownBy(() -> Invoice.create(customerId, "   ", issueDate, dueDate))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Invoice number cannot be empty");
    }

    @Test
    @DisplayName("Should fail when invoice number exceeds 50 characters")
    void shouldFailWhenInvoiceNumberExceedsMaxLength() {
        String longNumber = "INV-" + "1".repeat(50);

        assertThatThrownBy(() -> Invoice.create(customerId, longNumber, issueDate, dueDate))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Invoice number cannot exceed 50 characters");
    }

    @Test
    @DisplayName("Should fail when issue date is null")
    void shouldFailWhenIssueDateIsNull() {
        assertThatThrownBy(() -> Invoice.create(customerId, invoiceNumber, null, dueDate))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Issue date cannot be null");
    }

    @Test
    @DisplayName("Should fail when due date is null")
    void shouldFailWhenDueDateIsNull() {
        assertThatThrownBy(() -> Invoice.create(customerId, invoiceNumber, issueDate, null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Due date cannot be null");
    }

    @Test
    @DisplayName("Should fail when due date is before issue date")
    void shouldFailWhenDueDateIsBeforeIssueDate() {
        LocalDate earlierDueDate = issueDate.minusDays(1);

        assertThatThrownBy(() -> Invoice.create(customerId, invoiceNumber, issueDate, earlierDueDate))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Due date cannot be before issue date");
    }

    @Test
    @DisplayName("Should allow same issue and due date")
    void shouldAllowSameIssueAndDueDate() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, issueDate);

        assertThat(invoice.getIssueDate()).isEqualTo(invoice.getDueDate());
    }

    @Test
    @DisplayName("Should add line item to draft invoice")
    void shouldAddLineItemToDraftInvoice() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);

        invoice.addLineItem("Service A", BigDecimal.valueOf(2), Money.of(100.0));

        assertThat(invoice.getLineItems()).hasSize(1);
        assertThat(invoice.getLineItems().get(0).getDescription()).isEqualTo("Service A");
    }

    @Test
    @DisplayName("Should calculate totals when adding line items")
    void shouldCalculateTotalsWhenAddingLineItems() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);

        // Add line item: 2 x $100 = $200
        invoice.addLineItem("Service A", BigDecimal.valueOf(2), Money.of(100.0));
        // Add line item: 1 x $50 = $50
        invoice.addLineItem("Service B", BigDecimal.valueOf(1), Money.of(50.0));

        // Subtotal = $250
        // Tax (10%) = $25
        // Total = $275
        assertThat(invoice.getSubtotal().getAmount()).isEqualByComparingTo("250.00");
        assertThat(invoice.getTaxAmount().getAmount()).isEqualByComparingTo("25.00");
        assertThat(invoice.getTotalAmount().getAmount()).isEqualByComparingTo("275.00");
        assertThat(invoice.getBalanceDue().getAmount()).isEqualByComparingTo("0.00"); // Draft invoice
    }

    @Test
    @DisplayName("Should not add line item to sent invoice")
    void shouldNotAddLineItemToSentInvoice() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
        invoice.send();

        assertThatThrownBy(() -> invoice.addLineItem("Service B", BigDecimal.valueOf(1), Money.of(50.0)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot add line items to a non-draft invoice");
    }

    @Test
    @DisplayName("Should remove line item from draft invoice")
    void shouldRemoveLineItemFromDraftInvoice() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(2), Money.of(100.0));
        invoice.addLineItem("Service B", BigDecimal.valueOf(1), Money.of(50.0));

        // Set IDs for line items (simulating persistence)
        UUID lineItemId = UUID.randomUUID();
        invoice.getLineItems().get(0).setId(lineItemId);
        invoice.getLineItems().get(1).setId(UUID.randomUUID());

        invoice.removeLineItem(lineItemId);

        assertThat(invoice.getLineItems()).hasSize(1);
        assertThat(invoice.getLineItems().get(0).getDescription()).isEqualTo("Service B");
    }

    @Test
    @DisplayName("Should recalculate totals after removing line item")
    void shouldRecalculateTotalsAfterRemovingLineItem() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(2), Money.of(100.0));
        invoice.addLineItem("Service B", BigDecimal.valueOf(1), Money.of(50.0));

        // Set IDs for line items (simulating persistence)
        UUID lineItemId = UUID.randomUUID();
        invoice.getLineItems().get(0).setId(lineItemId);
        invoice.getLineItems().get(1).setId(UUID.randomUUID());

        invoice.removeLineItem(lineItemId);

        // Only Service B remains: $50 + 10% tax = $55
        assertThat(invoice.getSubtotal().getAmount()).isEqualByComparingTo("50.00");
        assertThat(invoice.getTaxAmount().getAmount()).isEqualByComparingTo("5.00");
        assertThat(invoice.getTotalAmount().getAmount()).isEqualByComparingTo("55.00");
    }

    @Test
    @DisplayName("Should not remove line item from sent invoice")
    void shouldNotRemoveLineItemFromSentInvoice() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
        UUID lineItemId = invoice.getLineItems().get(0).getId();
        invoice.send();

        assertThatThrownBy(() -> invoice.removeLineItem(lineItemId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot remove line items from a non-draft invoice");
    }

    @Test
    @DisplayName("Should fail when removing non-existent line item")
    void shouldFailWhenRemovingNonExistentLineItem() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));

        // Set ID for line item (simulating persistence)
        invoice.getLineItems().get(0).setId(UUID.randomUUID());

        UUID nonExistentId = UUID.randomUUID();

        assertThatThrownBy(() -> invoice.removeLineItem(nonExistentId))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Line item not found");
    }

    @Test
    @DisplayName("Should update invoice dates in draft status")
    void shouldUpdateInvoiceDatesInDraftStatus() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);

        LocalDate newIssueDate = issueDate.plusDays(1);
        LocalDate newDueDate = dueDate.plusDays(5);

        invoice.update(newIssueDate, newDueDate);

        assertThat(invoice.getIssueDate()).isEqualTo(newIssueDate);
        assertThat(invoice.getDueDate()).isEqualTo(newDueDate);
    }

    @Test
    @DisplayName("Should not update sent invoice")
    void shouldNotUpdateSentInvoice() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
        invoice.send();

        LocalDate newIssueDate = issueDate.plusDays(1);
        LocalDate newDueDate = dueDate.plusDays(5);

        assertThatThrownBy(() -> invoice.update(newIssueDate, newDueDate))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot update a non-draft invoice");
    }

    @Test
    @DisplayName("Should send invoice with line items")
    void shouldSendInvoiceWithLineItems() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(2), Money.of(100.0));

        invoice.send();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(invoice.getBalanceDue()).isEqualTo(invoice.getTotalAmount());
    }

    @Test
    @DisplayName("Should not send invoice without line items")
    void shouldNotSendInvoiceWithoutLineItems() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);

        assertThatThrownBy(() -> invoice.send())
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot send an invoice without line items");
    }

    @Test
    @DisplayName("Should not send already sent invoice")
    void shouldNotSendAlreadySentInvoice() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
        invoice.send();

        assertThatThrownBy(() -> invoice.send())
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Only draft invoices can be sent");
    }

    @Test
    @DisplayName("Should apply payment to sent invoice")
    void shouldApplyPaymentToSentInvoice() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
        invoice.send();

        Money totalAmount = invoice.getTotalAmount();
        Money paymentAmount = Money.of(50.0);

        invoice.applyPayment(paymentAmount);

        Money expectedBalance = totalAmount.subtract(paymentAmount);
        assertThat(invoice.getBalanceDue()).isEqualTo(expectedBalance);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
    }

    @Test
    @DisplayName("Should transition to PAID when balance is zero")
    void shouldTransitionToPaidWhenBalanceIsZero() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
        invoice.send();

        Money totalAmount = invoice.getTotalAmount();
        invoice.applyPayment(totalAmount);

        assertThat(invoice.getBalanceDue()).isEqualTo(Money.zero());
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("Should handle multiple partial payments")
    void shouldHandleMultiplePartialPayments() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
        invoice.send();

        Money totalAmount = invoice.getTotalAmount();

        invoice.applyPayment(Money.of(30.0));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SENT);

        invoice.applyPayment(Money.of(40.0));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SENT);

        Money remainingBalance = totalAmount.subtract(Money.of(70.0));
        invoice.applyPayment(remainingBalance);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getBalanceDue()).isEqualTo(Money.zero());
    }

    @Test
    @DisplayName("Should not apply payment to draft invoice")
    void shouldNotApplyPaymentToDraftInvoice() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));

        assertThatThrownBy(() -> invoice.applyPayment(Money.of(50.0)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Can only apply payments to sent invoices");
    }

    @Test
    @DisplayName("Should not apply zero payment")
    void shouldNotApplyZeroPayment() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
        invoice.send();

        assertThatThrownBy(() -> invoice.applyPayment(Money.zero()))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Payment amount must be greater than zero");
    }

    @Test
    @DisplayName("Should not apply negative payment")
    void shouldNotApplyNegativePayment() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
        invoice.send();

        assertThatThrownBy(() -> invoice.applyPayment(Money.of(-50.0)))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Payment amount must be greater than zero");
    }

    @Test
    @DisplayName("Should not apply payment exceeding balance")
    void shouldNotApplyPaymentExceedingBalance() {
        Invoice invoice = Invoice.create(customerId, invoiceNumber, issueDate, dueDate);
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
        invoice.send();

        Money excessivePayment = invoice.getBalanceDue().add(Money.of(1.0));

        assertThatThrownBy(() -> invoice.applyPayment(excessivePayment))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Payment amount cannot exceed balance due");
    }
}
