package com.osgiliath.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osgiliath.BaseIntegrationTest;
import com.osgiliath.application.invoice.AddLineItemCommand;
import com.osgiliath.application.invoice.CreateInvoiceCommand;
import com.osgiliath.application.payment.command.RecordPaymentCommand;
import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceStatus;
import com.osgiliath.domain.payment.Payment;
import com.osgiliath.domain.payment.PaymentMethod;
import com.osgiliath.domain.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for complete invoice lifecycle with new features
 * Tests the full flow from customer creation through invoice payment
 */
@DisplayName("Invoice Lifecycle Integration Test")
class InvoiceLifecycleIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should complete full invoice lifecycle with all features")
    void completeInvoiceLifecycleWithAllFeatures() throws Exception {
        // ===== STEP 1: Create Customer =====
        Customer customer = testDataBuilder.customer()
                .name("Tech Solutions Inc")
                .email("billing@techsolutions.com")
                .buildAndSave();

        // ===== STEP 2: Create Invoice in DRAFT status =====
        CreateInvoiceCommand createInvoiceCommand = new CreateInvoiceCommand(
                customer.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                List.of()
        );

        MvcResult invoiceResult = mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createInvoiceCommand)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(0.0))
                .andReturn();

        String invoiceJson = invoiceResult.getResponse().getContentAsString();
        UUID invoiceId = UUID.fromString(objectMapper.readTree(invoiceJson).get("id").asText());

        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);

        // ===== STEP 3: Add Line Items =====
        AddLineItemCommand lineItem1 = new AddLineItemCommand(
                invoiceId,
                "Web Development - 50 hours",
                "50",
                "100.00"
        );

        mockMvc.perform(post("/api/invoices/" + invoiceId + "/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lineItem1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Web Development - 50 hours"));

        AddLineItemCommand lineItem2 = new AddLineItemCommand(
                invoiceId,
                "Server Setup",
                "1",
                "500.00"
        );

        mockMvc.perform(post("/api/invoices/" + invoiceId + "/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lineItem2)))
                .andExpect(status().isCreated());

        // Verify invoice has line items
        Invoice invoiceWithItems = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(invoiceWithItems.getLineItems()).hasSize(2);
        // Total: (50 * 100 + 500) * 1.1 (with 10% tax) = 5500 * 1.1 = 6050
        assertThat(invoiceWithItems.getTotalAmount().getAmount()).isEqualByComparingTo("6050.00");

        // ===== STEP 4: Send Invoice (DRAFT → SENT) =====
        mockMvc.perform(post("/api/invoices/" + invoiceId + "/send"))
                .andExpect(status().isOk());

        Invoice sentInvoice = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(sentInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(sentInvoice.getBalanceDue().getAmount()).isEqualByComparingTo("6050.00");

        // ===== STEP 5: Get Invoice Balance =====
        mockMvc.perform(get("/api/invoices/" + invoiceId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(6050.00))
                .andExpect(jsonPath("$.balanceDue").value(6050.00))
                .andExpect(jsonPath("$.status").value("SENT"));

        // ===== STEP 6: Apply Partial Payment =====
        RecordPaymentCommand payment1 = new RecordPaymentCommand(
                invoiceId,
                new BigDecimal("3000.00"),
                LocalDate.now(),
                PaymentMethod.BANK_TRANSFER,
                "WIRE-001"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceStatus").value("SENT"))
                .andExpect(jsonPath("$.newBalanceDue").value(3050.00));

        // ===== STEP 7: Verify Balance Updated =====
        mockMvc.perform(get("/api/invoices/" + invoiceId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(6050.00))
                .andExpect(jsonPath("$.balanceDue").value(3050.00))
                .andExpect(jsonPath("$.status").value("SENT"));

        Invoice afterPartialPayment = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(afterPartialPayment.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(afterPartialPayment.getBalanceDue().getAmount()).isEqualByComparingTo("3050.00");

        // ===== STEP 8: Apply Final Payment =====
        RecordPaymentCommand payment2 = new RecordPaymentCommand(
                invoiceId,
                new BigDecimal("3050.00"),
                LocalDate.now(),
                PaymentMethod.CREDIT_CARD,
                "CC-002"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceStatus").value("PAID"))
                .andExpect(jsonPath("$.newBalanceDue").value(0.0));

        // ===== STEP 9: Verify Status → PAID =====
        Invoice paidInvoice = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paidInvoice.getBalanceDue().isZero()).isTrue();

        mockMvc.perform(get("/api/invoices/" + invoiceId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(6050.00))
                .andExpect(jsonPath("$.balanceDue").value(0.0))
                .andExpect(jsonPath("$.status").value("PAID"));

        // Verify payments
        List<Payment> payments = paymentRepository.findByInvoiceId(invoiceId);
        assertThat(payments).hasSize(2);

        BigDecimal totalPaid = payments.stream()
                .map(p -> p.getAmount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPaid).isEqualByComparingTo("6050.00");
    }

    @Test
    @DisplayName("Should not allow sending invoice without line items")
    void cannotSendInvoiceWithoutLineItems() throws Exception {
        // Create customer
        Customer customer = testDataBuilder.customer()
                .name("Empty Invoice Corp")
                .email("empty@example.com")
                .buildAndSave();

        // Create invoice without line items
        CreateInvoiceCommand createInvoiceCommand = new CreateInvoiceCommand(
                customer.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                List.of()
        );

        MvcResult invoiceResult = mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createInvoiceCommand)))
                .andExpect(status().isCreated())
                .andReturn();

        String invoiceJson = invoiceResult.getResponse().getContentAsString();
        UUID invoiceId = UUID.fromString(objectMapper.readTree(invoiceJson).get("id").asText());

        // Attempt to send invoice without line items
        mockMvc.perform(post("/api/invoices/" + invoiceId + "/send"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot send invoice without line items")));

        // Verify invoice is still in DRAFT status
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    @DisplayName("Should cancel invoice successfully")
    void cancelInvoiceFlow() throws Exception {
        // Create and send invoice
        Customer customer = testDataBuilder.customer()
                .name("Cancel Test Corp")
                .email("cancel@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildWithLineItemsAndSave();

        // Send invoice
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/send"))
                .andExpect(status().isOk());

        Invoice sentInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(sentInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
        Money balanceBeforeCancel = sentInvoice.getBalanceDue();
        assertThat(balanceBeforeCancel.getAmount()).isGreaterThan(BigDecimal.ZERO);

        // Cancel invoice
        String cancelRequest = """
                {
                    "reason": "Customer requested cancellation"
                }
                """;

        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelRequest))
                .andExpect(status().isOk());

        // Verify status = CANCELLED and balance = 0
        Invoice cancelledInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(cancelledInvoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(cancelledInvoice.getBalanceDue().isZero()).isTrue();

        mockMvc.perform(get("/api/invoices/" + invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.balanceDue").value(0.0));
    }

    @Test
    @DisplayName("Should handle mark as paid endpoint")
    void shouldMarkInvoiceAsPaidManually() throws Exception {
        // Create and send invoice
        Customer customer = testDataBuilder.customer()
                .name("Manual Paid Corp")
                .email("manual@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildSentAndSave();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(invoice.getBalanceDue().getAmount()).isGreaterThan(BigDecimal.ZERO);

        // Mark as paid manually
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/mark-paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        Invoice paidInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paidInvoice.getBalanceDue().isZero()).isTrue();
    }

    @Test
    @DisplayName("Should delete draft invoice successfully")
    void shouldDeleteDraftInvoice() throws Exception {
        Customer customer = testDataBuilder.customer()
                .name("Delete Test Corp")
                .email("delete@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildAndSave();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        UUID invoiceId = invoice.getId();

        // Delete invoice
        mockMvc.perform(delete("/api/invoices/" + invoiceId))
                .andExpect(status().isNoContent());

        // Verify invoice is deleted
        assertThat(invoiceRepository.findById(invoiceId)).isEmpty();
    }

    @Test
    @DisplayName("Should not delete sent invoice")
    void shouldNotDeleteSentInvoice() throws Exception {
        Customer customer = testDataBuilder.customer()
                .name("No Delete Corp")
                .email("nodelete@example.com")
                .buildAndSave();

        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildSentAndSave();

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SENT);

        // Attempt to delete sent invoice
        mockMvc.perform(delete("/api/invoices/" + invoice.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Can only delete draft invoices")));

        // Verify invoice still exists
        assertThat(invoiceRepository.findById(invoice.getId())).isPresent();
    }
}
