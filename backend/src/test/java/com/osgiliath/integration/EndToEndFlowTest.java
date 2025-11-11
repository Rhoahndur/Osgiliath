package com.osgiliath.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osgiliath.BaseIntegrationTest;
import com.osgiliath.application.customer.command.CreateCustomerCommand;
import com.osgiliath.application.invoice.AddLineItemCommand;
import com.osgiliath.application.invoice.CreateInvoiceCommand;
import com.osgiliath.application.payment.command.RecordPaymentCommand;
import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceStatus;
import com.osgiliath.domain.payment.Payment;
import com.osgiliath.domain.payment.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
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
 * End-to-end integration test covering complete invoice lifecycle
 *
 * Flow:
 * 1. Create customer
 * 2. Create invoice for customer
 * 3. Add line items to invoice
 * 4. Send invoice
 * 5. Make partial payment
 * 6. Make second partial payment
 * 7. Make final payment (invoice becomes PAID)
 */
@DisplayName("End-to-End Flow Test")
class EndToEndFlowTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should complete full invoice lifecycle from customer creation to final payment")
    void shouldCompleteFullInvoiceLifecycle() throws Exception {
        // ===== STEP 1: Create Customer =====
        CreateCustomerCommand createCustomerCommand = new CreateCustomerCommand(
                "Acme Corporation",
                "billing@acme.com",
                "555-0100",
                "100 Business Park Drive"
        );

        MvcResult customerResult = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCustomerCommand)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Acme Corporation"))
                .andExpect(jsonPath("$.email").value("billing@acme.com"))
                .andReturn();

        String customerJson = customerResult.getResponse().getContentAsString();
        UUID customerId = UUID.fromString(objectMapper.readTree(customerJson).get("id").asText());

        // Verify customer exists
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        assertThat(customer.getName()).isEqualTo("Acme Corporation");

        // ===== STEP 2: Create Invoice =====
        CreateInvoiceCommand createInvoiceCommand = new CreateInvoiceCommand(
                customerId,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                List.of()
        );

        MvcResult invoiceResult = mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createInvoiceCommand)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2024-001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.totalAmount").value(0.0))
                .andReturn();

        String invoiceJson = invoiceResult.getResponse().getContentAsString();
        UUID invoiceId = UUID.fromString(objectMapper.readTree(invoiceJson).get("id").asText());

        // Verify invoice exists in DRAFT status
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);

        // ===== STEP 3: Add Line Items =====
        // Line Item 1: Consulting Services - 40 hours @ $150/hour = $6,000
        AddLineItemCommand lineItem1 = new AddLineItemCommand(
                invoiceId,
                "Consulting Services - Project Analysis",
                "40",
                "150.00"
        );

        mockMvc.perform(post("/api/invoices/" + invoiceId + "/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lineItem1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems.length()").value(1))
                .andExpect(jsonPath("$.subtotal").value(6000.00));

        // Line Item 2: Development Services - 60 hours @ $200/hour = $12,000
        AddLineItemCommand lineItem2 = new AddLineItemCommand(
                invoiceId,
                "Software Development Services",
                "60",
                "200.00"
        );

        mockMvc.perform(post("/api/invoices/" + invoiceId + "/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lineItem2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems.length()").value(2))
                .andExpect(jsonPath("$.subtotal").value(18000.00))
                .andExpect(jsonPath("$.taxAmount").value(1800.00))  // 10% tax
                .andExpect(jsonPath("$.totalAmount").value(19800.00));

        // Line Item 3: Documentation - $500
        AddLineItemCommand lineItem3 = new AddLineItemCommand(
                invoiceId,
                "Technical Documentation",
                "1",
                "500.00"
        );

        mockMvc.perform(post("/api/invoices/" + invoiceId + "/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lineItem3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems.length()").value(3))
                .andExpect(jsonPath("$.subtotal").value(18500.00))
                .andExpect(jsonPath("$.taxAmount").value(1850.00))
                .andExpect(jsonPath("$.totalAmount").value(20350.00));

        // Verify line items in database
        Invoice invoiceWithItems = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(invoiceWithItems.getLineItems()).hasSize(3);
        assertThat(invoiceWithItems.getTotalAmount().getAmount()).isEqualByComparingTo("20350.00");

        // ===== STEP 4: Send Invoice =====
        mockMvc.perform(post("/api/invoices/" + invoiceId + "/send"))
                .andExpect(status().isOk());

        // Verify invoice status changed to SENT
        Invoice sentInvoice = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(sentInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(sentInvoice.getBalanceDue()).isEqualTo(sentInvoice.getTotalAmount());
        assertThat(sentInvoice.getBalanceDue().getAmount()).isEqualByComparingTo("20350.00");

        // Verify cannot add line items after sending
        AddLineItemCommand extraItem = new AddLineItemCommand(
                invoiceId,
                "Extra Service",
                "1",
                "100.00"
        );

        mockMvc.perform(post("/api/invoices/" + invoiceId + "/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraItem)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot add line items to a non-draft invoice")));

        // ===== STEP 5: First Partial Payment (40% = $8,140) =====
        RecordPaymentCommand payment1 = new RecordPaymentCommand(
                invoiceId,
                new BigDecimal("8140.00"),
                LocalDate.now(),
                PaymentMethod.BANK_TRANSFER,
                "WIRE-20240101-001"
        );

        MvcResult payment1Result = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceStatus").value("SENT"))
                .andExpect(jsonPath("$.newBalanceDue").value(12210.00))
                .andReturn();

        String payment1Json = payment1Result.getResponse().getContentAsString();
        UUID payment1Id = UUID.fromString(objectMapper.readTree(payment1Json).get("paymentId").asText());

        // Verify first payment
        Payment savedPayment1 = paymentRepository.findById(payment1Id).orElseThrow();
        assertThat(savedPayment1.getAmount().getAmount()).isEqualByComparingTo("8140.00");
        assertThat(savedPayment1.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);

        Invoice afterPayment1 = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(afterPayment1.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(afterPayment1.getBalanceDue().getAmount()).isEqualByComparingTo("12210.00");

        // ===== STEP 6: Second Partial Payment (35% = $7,122.50) =====
        RecordPaymentCommand payment2 = new RecordPaymentCommand(
                invoiceId,
                new BigDecimal("7122.50"),
                LocalDate.now(),
                PaymentMethod.CREDIT_CARD,
                "CC-VISA-4242"
        );

        MvcResult payment2Result = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceStatus").value("SENT"))
                .andExpect(jsonPath("$.newBalanceDue").value(5087.50))
                .andReturn();

        String payment2Json = payment2Result.getResponse().getContentAsString();
        UUID payment2Id = UUID.fromString(objectMapper.readTree(payment2Json).get("paymentId").asText());

        // Verify second payment
        Payment savedPayment2 = paymentRepository.findById(payment2Id).orElseThrow();
        assertThat(savedPayment2.getAmount().getAmount()).isEqualByComparingTo("7122.50");

        Invoice afterPayment2 = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(afterPayment2.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(afterPayment2.getBalanceDue().getAmount()).isEqualByComparingTo("5087.50");

        // ===== STEP 7: Final Payment (remaining balance) =====
        RecordPaymentCommand payment3 = new RecordPaymentCommand(
                invoiceId,
                new BigDecimal("5087.50"),
                LocalDate.now(),
                PaymentMethod.CHECK,
                "CHECK-789456"
        );

        MvcResult payment3Result = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceStatus").value("PAID"))
                .andExpect(jsonPath("$.newBalanceDue").value(0.0))
                .andReturn();

        String payment3Json = payment3Result.getResponse().getContentAsString();
        UUID payment3Id = UUID.fromString(objectMapper.readTree(payment3Json).get("paymentId").asText());

        // Verify final payment and invoice status
        Payment savedPayment3 = paymentRepository.findById(payment3Id).orElseThrow();
        assertThat(savedPayment3.getAmount().getAmount()).isEqualByComparingTo("5087.50");

        Invoice paidInvoice = invoiceRepository.findById(invoiceId).orElseThrow();
        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paidInvoice.getBalanceDue().getAmount()).isEqualByComparingTo("0.00");

        // ===== VERIFICATION: List all payments for the invoice =====
        mockMvc.perform(get("/api/invoices/" + invoiceId + "/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        List<Payment> allPayments = paymentRepository.findByInvoiceId(invoiceId);
        assertThat(allPayments).hasSize(3);

        BigDecimal totalPaid = allPayments.stream()
                .map(p -> p.getAmount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalPaid).isEqualByComparingTo("20350.00");

        // ===== VERIFICATION: Try to make another payment (should fail) =====
        RecordPaymentCommand extraPayment = new RecordPaymentCommand(
                invoiceId,
                new BigDecimal("100.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                "EXTRA"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraPayment)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Can only apply payments to sent invoices")));

        // ===== VERIFICATION: Check invoice balance endpoint =====
        mockMvc.perform(get("/api/invoices/" + invoiceId + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(20350.00))
                .andExpect(jsonPath("$.balanceDue").value(0.0))
                .andExpect(jsonPath("$.status").value("PAID"));

        // ===== VERIFICATION: Get complete invoice details =====
        mockMvc.perform(get("/api/invoices/" + invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-2024-001"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.lineItems.length()").value(3))
                .andExpect(jsonPath("$.subtotal").value(18500.00))
                .andExpect(jsonPath("$.taxAmount").value(1850.00))
                .andExpect(jsonPath("$.totalAmount").value(20350.00))
                .andExpect(jsonPath("$.balanceDue").value(0.0));

        // ===== VERIFICATION: Get customer details =====
        mockMvc.perform(get("/api/customers/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.name").value("Acme Corporation"))
                .andExpect(jsonPath("$.email").value("billing@acme.com"));

        // ===== FINAL ASSERTIONS =====
        // Verify database state
        Customer finalCustomer = customerRepository.findById(customerId).orElseThrow();
        Invoice finalInvoice = invoiceRepository.findById(invoiceId).orElseThrow();
        List<Payment> finalPayments = paymentRepository.findByInvoiceId(invoiceId);

        assertThat(finalCustomer.getName()).isEqualTo("Acme Corporation");
        assertThat(finalInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(finalInvoice.getLineItems()).hasSize(3);
        assertThat(finalInvoice.getBalanceDue().isZero()).isTrue();
        assertThat(finalPayments).hasSize(3);

        // Verify payment methods diversity
        assertThat(finalPayments).extracting(Payment::getPaymentMethod)
                .containsExactlyInAnyOrder(
                        PaymentMethod.BANK_TRANSFER,
                        PaymentMethod.CREDIT_CARD,
                        PaymentMethod.CHECK
                );
    }

    @Test
    @DisplayName("Should handle complete flow with single full payment")
    void shouldHandleCompleteFlowWithSingleFullPayment() throws Exception {
        // Create customer
        Customer customer = testDataBuilder.customer()
                .name("Quick Pay Corp")
                .email("quickpay@example.com")
                .buildAndSave();

        // Create invoice with line items
        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .invoiceNumber("INV-QUICK-001")
                .buildWithLineItemsAndSave();

        BigDecimal totalAmount = invoice.getTotalAmount().getAmount();

        // Send invoice
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/send"))
                .andExpect(status().isOk());

        // Make full payment immediately
        RecordPaymentCommand fullPayment = new RecordPaymentCommand(
                invoice.getId(),
                totalAmount,
                LocalDate.now(),
                PaymentMethod.BANK_TRANSFER,
                "WIRE-FULL-PAYMENT"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullPayment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceStatus").value("PAID"))
                .andExpect(jsonPath("$.newBalanceDue").value(0.0));

        // Verify final state
        Invoice paidInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paidInvoice.getBalanceDue().isZero()).isTrue();

        List<Payment> payments = paymentRepository.findByInvoiceId(invoice.getId());
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getAmount().getAmount()).isEqualByComparingTo(totalAmount);
    }
}
