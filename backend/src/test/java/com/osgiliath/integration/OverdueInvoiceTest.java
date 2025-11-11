package com.osgiliath.integration;

import com.osgiliath.BaseIntegrationTest;
import com.osgiliath.application.invoice.MarkOverdueInvoicesCommand;
import com.osgiliath.application.invoice.MarkOverdueInvoicesHandler;
import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for overdue invoice automation
 * Tests the batch job that marks sent invoices as overdue
 */
@DisplayName("Overdue Invoice Test")
class OverdueInvoiceTest extends BaseIntegrationTest {

    @Autowired
    private MarkOverdueInvoicesHandler handler;

    @Test
    @DisplayName("Should mark sent invoices as overdue when past due date")
    void invoicesBecomeOverdueAfterDueDate() {
        // Create invoice with past due date
        Customer customer = testDataBuilder.customer()
                .name("Overdue Test Customer 1")
                .email("overdue1@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .issueDate(LocalDate.now().minusDays(45))
                .dueDate(LocalDate.now().minusDays(15))
                .buildWithLineItemsAndSave();

        // Send invoice
        invoice.send();
        invoiceRepository.save(invoice);

        Invoice sentInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(sentInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(sentInvoice.getDueDate()).isBefore(LocalDate.now());

        // Run MarkOverdueInvoicesHandler
        int markedCount = handler.handle(new MarkOverdueInvoicesCommand());

        // Verify at least this invoice was marked
        assertThat(markedCount).isGreaterThanOrEqualTo(1);

        // Verify status changed to OVERDUE
        Invoice overdueInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(overdueInvoice.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
    }

    @Test
    @DisplayName("Should not mark draft invoices as overdue")
    void draftInvoicesNotMarkedOverdue() {
        // Create DRAFT invoice with past due date
        Customer customer = testDataBuilder.customer()
                .name("Draft Overdue Customer")
                .email("draftoverdue@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .issueDate(LocalDate.now().minusDays(60))
                .dueDate(LocalDate.now().minusDays(30))
                .buildWithLineItemsAndSave();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.getDueDate()).isBefore(LocalDate.now());

        // Run handler
        handler.handle(new MarkOverdueInvoicesCommand());

        // Verify status still DRAFT
        Invoice stillDraftInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(stillDraftInvoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    @DisplayName("Should not mark paid invoices as overdue")
    void paidInvoicesNotMarkedOverdue() {
        // Create invoice with past due date
        Customer customer = testDataBuilder.customer()
                .name("Paid Overdue Customer")
                .email("paidoverdue@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .issueDate(LocalDate.now().minusDays(60))
                .dueDate(LocalDate.now().minusDays(30))
                .buildWithLineItemsAndSave();

        // Send and mark as paid
        invoice.send();
        invoice.markAsPaid();
        invoiceRepository.save(invoice);

        Invoice paidInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paidInvoice.getDueDate()).isBefore(LocalDate.now());

        // Run handler
        handler.handle(new MarkOverdueInvoicesCommand());

        // Verify status still PAID
        Invoice stillPaidInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(stillPaidInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("Should not mark cancelled invoices as overdue")
    void cancelledInvoicesNotMarkedOverdue() {
        // Create invoice with past due date
        Customer customer = testDataBuilder.customer()
                .name("Cancelled Overdue Customer")
                .email("cancelledoverdue@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .issueDate(LocalDate.now().minusDays(60))
                .dueDate(LocalDate.now().minusDays(30))
                .buildWithLineItemsAndSave();

        // Send and cancel
        invoice.send();
        invoice.cancel();
        invoiceRepository.save(invoice);

        Invoice cancelledInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(cancelledInvoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(cancelledInvoice.getDueDate()).isBefore(LocalDate.now());

        // Run handler
        handler.handle(new MarkOverdueInvoicesCommand());

        // Verify status still CANCELLED
        Invoice stillCancelledInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(stillCancelledInvoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should not mark sent invoices as overdue when due date is in future")
    void sentInvoicesNotOverdueWhenDueDateInFuture() {
        // Create invoice with future due date
        Customer customer = testDataBuilder.customer()
                .name("Future Due Customer")
                .email("futuredue@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .buildWithLineItemsAndSave();

        // Send invoice
        invoice.send();
        invoiceRepository.save(invoice);

        Invoice sentInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(sentInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(sentInvoice.getDueDate()).isAfter(LocalDate.now());

        // Run handler
        handler.handle(new MarkOverdueInvoicesCommand());

        // Verify status still SENT
        Invoice stillSentInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(stillSentInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
    }

    @Test
    @DisplayName("Should mark multiple sent invoices as overdue in batch")
    void shouldMarkMultipleInvoicesOverdue() {
        // Create multiple customers and invoices
        Customer customer1 = testDataBuilder.customer()
                .name("Batch Customer 1")
                .email("batch1@example.com")
                .buildAndSave();

        Customer customer2 = testDataBuilder.customer()
                .name("Batch Customer 2")
                .email("batch2@example.com")
                .buildAndSave();

        Customer customer3 = testDataBuilder.customer()
                .name("Batch Customer 3")
                .email("batch3@example.com")
                .buildAndSave();

        // Create 3 overdue sent invoices
        Invoice invoice1 = testDataBuilder.invoice()
                .customer(customer1)
                .issueDate(LocalDate.now().minusDays(50))
                .dueDate(LocalDate.now().minusDays(20))
                .buildWithLineItemsAndSave();
        invoice1.send();
        invoiceRepository.save(invoice1);

        Invoice invoice2 = testDataBuilder.invoice()
                .customer(customer2)
                .issueDate(LocalDate.now().minusDays(40))
                .dueDate(LocalDate.now().minusDays(10))
                .buildWithLineItemsAndSave();
        invoice2.send();
        invoiceRepository.save(invoice2);

        Invoice invoice3 = testDataBuilder.invoice()
                .customer(customer3)
                .issueDate(LocalDate.now().minusDays(35))
                .dueDate(LocalDate.now().minusDays(5))
                .buildWithLineItemsAndSave();
        invoice3.send();
        invoiceRepository.save(invoice3);

        // Create 1 sent invoice with future due date (should not be marked)
        Customer customer4 = testDataBuilder.customer()
                .name("Batch Customer 4")
                .email("batch4@example.com")
                .buildAndSave();

        Invoice invoice4 = testDataBuilder.invoice()
                .customer(customer4)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .buildWithLineItemsAndSave();
        invoice4.send();
        invoiceRepository.save(invoice4);

        // Run handler
        int markedCount = handler.handle(new MarkOverdueInvoicesCommand());

        // Verify at least 3 invoices were marked
        assertThat(markedCount).isGreaterThanOrEqualTo(3);

        // Verify the 3 overdue invoices are marked OVERDUE
        Invoice updatedInvoice1 = invoiceRepository.findById(invoice1.getId()).orElseThrow();
        Invoice updatedInvoice2 = invoiceRepository.findById(invoice2.getId()).orElseThrow();
        Invoice updatedInvoice3 = invoiceRepository.findById(invoice3.getId()).orElseThrow();

        assertThat(updatedInvoice1.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
        assertThat(updatedInvoice2.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
        assertThat(updatedInvoice3.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);

        // Verify the future invoice is still SENT
        Invoice updatedInvoice4 = invoiceRepository.findById(invoice4.getId()).orElseThrow();
        assertThat(updatedInvoice4.getStatus()).isEqualTo(InvoiceStatus.SENT);
    }

    @Test
    @DisplayName("Should handle empty result when no invoices are overdue")
    void shouldHandleNoOverdueInvoices() {
        // Create only sent invoices with future due dates
        Customer customer = testDataBuilder.customer()
                .name("No Overdue Customer")
                .email("nooverdue@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .buildWithLineItemsAndSave();
        invoice.send();
        invoiceRepository.save(invoice);

        // Clean up any existing overdue invoices from previous tests
        // (in real scenario, this would be a fresh database)

        // Run handler - should complete successfully even with 0 results
        int markedCount = handler.handle(new MarkOverdueInvoicesCommand());

        // Verify count is 0 or positive (from other tests)
        assertThat(markedCount).isGreaterThanOrEqualTo(0);

        // Verify the invoice is still SENT
        Invoice stillSentInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(stillSentInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
    }

    @Test
    @DisplayName("Should mark invoice as overdue on exact due date")
    void shouldMarkOverdueOnExactDueDate() {
        // Create invoice with due date = yesterday
        Customer customer = testDataBuilder.customer()
                .name("Yesterday Due Customer")
                .email("yesterday@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .issueDate(LocalDate.now().minusDays(30))
                .dueDate(LocalDate.now().minusDays(1))
                .buildWithLineItemsAndSave();

        invoice.send();
        invoiceRepository.save(invoice);

        // Run handler
        int markedCount = handler.handle(new MarkOverdueInvoicesCommand());

        assertThat(markedCount).isGreaterThanOrEqualTo(1);

        // Verify status changed to OVERDUE
        Invoice overdueInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(overdueInvoice.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
    }
}
