package com.osgiliath.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osgiliath.BaseIntegrationTest;
import com.osgiliath.application.payment.command.RecordPaymentCommand;
import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceStatus;
import com.osgiliath.domain.payment.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for business rules validation
 * Tests critical business constraints and domain rules
 */
@DisplayName("Business Rules Validation Test")
class BusinessRulesValidationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should not allow deleting customer with invoices")
    void cannotDeleteCustomerWithInvoices() throws Exception {
        // Create customer with invoice
        Customer customer = testDataBuilder.customer()
                .name("Customer With Invoices")
                .email("withinvoices@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildAndSave();

        assertThat(invoice.getCustomerId()).isEqualTo(customer.getId());

        // Attempt to delete customer
        mockMvc.perform(delete("/api/customers/" + customer.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot delete customer with existing invoices")));

        // Verify customer still exists
        assertThat(customerRepository.findById(customer.getId())).isPresent();
    }

    @Test
    @DisplayName("Should allow deleting customer without invoices")
    void canDeleteCustomerWithoutInvoices() throws Exception {
        // Create customer without invoices
        Customer customer = testDataBuilder.customer()
                .name("Customer Without Invoices")
                .email("withoutinvoices@example.com")
                .buildAndSave();

        // Delete customer
        mockMvc.perform(delete("/api/customers/" + customer.getId()))
                .andExpect(status().isNoContent());

        // Verify customer is deleted
        assertThat(customerRepository.findById(customer.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should not allow payment to draft invoice")
    void cannotApplyPaymentToDraftInvoice() throws Exception {
        // Create DRAFT invoice
        Customer customer = testDataBuilder.customer()
                .name("Draft Invoice Customer")
                .email("draft@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildWithLineItemsAndSave();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);

        // Attempt payment to draft invoice
        RecordPaymentCommand paymentCommand = new RecordPaymentCommand(
                invoice.getId(),
                new BigDecimal("100.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                "INVALID-001"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCommand)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Can only apply payments to sent invoices")));

        // Verify no payments were created
        assertThat(paymentRepository.findByInvoiceId(invoice.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should not allow payment to cancelled invoice")
    void cannotApplyPaymentToCancelledInvoice() throws Exception {
        // Create and send invoice
        Customer customer = testDataBuilder.customer()
                .name("Cancelled Invoice Customer")
                .email("cancelled@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildSentAndSave();

        // Cancel invoice
        String cancelRequest = """
                {
                    "reason": "Test cancellation"
                }
                """;

        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelRequest))
                .andExpect(status().isOk());

        Invoice cancelledInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(cancelledInvoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);

        // Attempt payment to cancelled invoice
        RecordPaymentCommand paymentCommand = new RecordPaymentCommand(
                invoice.getId(),
                new BigDecimal("100.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                "INVALID-002"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCommand)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Can only apply payments to sent invoices")));

        // Verify no payments were created
        assertThat(paymentRepository.findByInvoiceId(invoice.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should not allow payment to paid invoice")
    void cannotApplyPaymentToPaidInvoice() throws Exception {
        // Create and send invoice
        Customer customer = testDataBuilder.customer()
                .name("Paid Invoice Customer")
                .email("paid@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildSentAndSave();

        // Make full payment
        RecordPaymentCommand fullPayment = new RecordPaymentCommand(
                invoice.getId(),
                invoice.getTotalAmount().getAmount(),
                LocalDate.now(),
                PaymentMethod.BANK_TRANSFER,
                "FULL-PAYMENT-001"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullPayment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceStatus").value("PAID"));

        Invoice paidInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);

        // Attempt another payment to paid invoice
        RecordPaymentCommand extraPayment = new RecordPaymentCommand(
                invoice.getId(),
                new BigDecimal("50.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                "EXTRA-001"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraPayment)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Can only apply payments to sent invoices")));
    }

    @Test
    @DisplayName("Should not allow payment exceeding balance")
    void cannotApplyPaymentExceedingBalance() throws Exception {
        // Create and send invoice
        Customer customer = testDataBuilder.customer()
                .name("Overpayment Customer")
                .email("overpay@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildSentAndSave();

        BigDecimal totalAmount = invoice.getTotalAmount().getAmount();
        BigDecimal overpayment = totalAmount.add(new BigDecimal("1000.00"));

        // Attempt payment exceeding balance
        RecordPaymentCommand paymentCommand = new RecordPaymentCommand(
                invoice.getId(),
                overpayment,
                LocalDate.now(),
                PaymentMethod.CREDIT_CARD,
                "OVERPAY-001"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCommand)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Payment amount exceeds balance due")));

        // Verify no payment was created
        assertThat(paymentRepository.findByInvoiceId(invoice.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should allow payment to overdue invoice")
    void canApplyPaymentToOverdueInvoice() throws Exception {
        // Create invoice with past due date
        Customer customer = testDataBuilder.customer()
                .name("Overdue Customer")
                .email("overdue@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .issueDate(LocalDate.now().minusDays(60))
                .dueDate(LocalDate.now().minusDays(30))
                .buildWithLineItemsAndSave();

        // Send invoice
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/send"))
                .andExpect(status().isOk());

        // Manually set to OVERDUE status (simulating the batch job)
        Invoice sentInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        sentInvoice.setStatus(InvoiceStatus.OVERDUE);
        invoiceRepository.save(sentInvoice);

        Invoice overdueInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(overdueInvoice.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);

        // Apply payment to overdue invoice
        RecordPaymentCommand paymentCommand = new RecordPaymentCommand(
                invoice.getId(),
                new BigDecimal("100.00"),
                LocalDate.now(),
                PaymentMethod.BANK_TRANSFER,
                "OVERDUE-PAYMENT-001"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCommand)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceStatus").value("OVERDUE"));

        // Verify payment was created
        assertThat(paymentRepository.findByInvoiceId(invoice.getId())).hasSize(1);

        // Verify balance updated
        Invoice invoiceAfterPayment = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(invoiceAfterPayment.getBalanceDue().getAmount())
                .isLessThan(overdueInvoice.getTotalAmount().getAmount());
    }

    @Test
    @DisplayName("Should not allow adding line items to sent invoice")
    void cannotAddLineItemsToSentInvoice() throws Exception {
        // Create and send invoice
        Customer customer = testDataBuilder.customer()
                .name("Sent Invoice Customer")
                .email("sent@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildSentAndSave();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SENT);

        // Attempt to add line item to sent invoice
        String lineItemRequest = """
                {
                    "invoiceId": "%s",
                    "description": "Extra Service",
                    "quantity": "1",
                    "unitPrice": "100.00"
                }
                """.formatted(invoice.getId());

        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lineItemRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot add line items to a non-draft invoice")));
    }

    @Test
    @DisplayName("Should enforce minimum payment amount")
    void shouldRejectNegativeOrZeroPayment() throws Exception {
        // Create and send invoice
        Customer customer = testDataBuilder.customer()
                .name("Zero Payment Customer")
                .email("zeropay@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildSentAndSave();

        // Attempt zero payment
        RecordPaymentCommand zeroPayment = new RecordPaymentCommand(
                invoice.getId(),
                BigDecimal.ZERO,
                LocalDate.now(),
                PaymentMethod.CASH,
                "ZERO-001"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroPayment)))
                .andExpect(status().isBadRequest());

        // Attempt negative payment
        RecordPaymentCommand negativePayment = new RecordPaymentCommand(
                invoice.getId(),
                new BigDecimal("-50.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                "NEG-001"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativePayment)))
                .andExpect(status().isBadRequest());

        // Verify no payments created
        assertThat(paymentRepository.findByInvoiceId(invoice.getId())).isEmpty();
    }
}
