package com.osgiliath.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osgiliath.BaseIntegrationTest;
import com.osgiliath.application.payment.dto.RecordPaymentRequest;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceStatus;
import com.osgiliath.domain.payment.Payment;
import com.osgiliath.domain.payment.PaymentMethod;
import com.osgiliath.domain.shared.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for Payment API endpoints Tests payment recording and invoice balance updates
 */
@DisplayName("Payment Integration Tests")
class PaymentIntegrationTest extends BaseIntegrationTest {

    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should record payment and update invoice balance")
    void shouldRecordPaymentAndUpdateInvoiceBalance() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice().buildSentAndSave();

        Money originalBalance = invoice.getBalanceDue();
        Money paymentAmount = Money.of(50.0);

        RecordPaymentRequest request =
                new RecordPaymentRequest(
                        paymentAmount.getAmount(),
                        LocalDate.now(),
                        PaymentMethod.BANK_TRANSFER,
                        "REF-12345");

        // When & Then
        MvcResult result =
                mockMvc.perform(
                                post("/api/invoices/" + invoice.getId() + "/payments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.invoiceId").value(invoice.getId().toString()))
                        .andExpect(jsonPath("$.amount").value(50.0))
                        .andExpect(jsonPath("$.updatedInvoiceStatus").value("SENT"))
                        .andReturn();

        // Verify payment was saved
        String responseBody = result.getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(responseBody).get("id").asText();

        Payment savedPayment = paymentRepository.findById(UUID.fromString(paymentId)).orElseThrow();

        assertThat(savedPayment.getAmount()).isEqualTo(paymentAmount);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);

        // Verify invoice balance was updated
        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();

        Money expectedBalance = originalBalance.subtract(paymentAmount);
        assertThat(updatedInvoice.getBalanceDue()).isEqualTo(expectedBalance);
        assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
    }

    @Test
    @DisplayName("Should transition invoice to PAID when full payment received")
    void shouldTransitionInvoiceToPaidWhenFullPaymentReceived() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice().buildSentAndSave();

        Money totalAmount = invoice.getTotalAmount();

        RecordPaymentRequest request =
                new RecordPaymentRequest(
                        totalAmount.getAmount(),
                        LocalDate.now(),
                        PaymentMethod.BANK_TRANSFER,
                        "REF-FULL");

        // When & Then
        mockMvc.perform(
                        post("/api/invoices/" + invoice.getId() + "/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.updatedInvoiceStatus").value("PAID"))
                .andExpect(jsonPath("$.updatedInvoiceBalance").value(0.0));

        // Verify invoice status
        Invoice paidInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();

        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paidInvoice.getBalanceDue()).isEqualTo(Money.zero());
    }

    @Test
    @DisplayName("Should handle multiple partial payments")
    void shouldHandleMultiplePartialPayments() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice().buildSentAndSave();

        Money totalAmount = invoice.getTotalAmount();

        // First payment: 40%
        RecordPaymentRequest payment1 =
                new RecordPaymentRequest(
                        totalAmount.multiply(new BigDecimal("0.4")).getAmount(),
                        LocalDate.now(),
                        PaymentMethod.BANK_TRANSFER,
                        "REF-1");

        mockMvc.perform(
                        post("/api/invoices/" + invoice.getId() + "/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payment1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.updatedInvoiceStatus").value("SENT"));

        // Second payment: 30%
        RecordPaymentRequest payment2 =
                new RecordPaymentRequest(
                        totalAmount.multiply(new BigDecimal("0.3")).getAmount(),
                        LocalDate.now(),
                        PaymentMethod.CREDIT_CARD,
                        "REF-2");

        mockMvc.perform(
                        post("/api/invoices/" + invoice.getId() + "/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payment2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.updatedInvoiceStatus").value("SENT"));

        // Third payment: 30% (total now 100%)
        RecordPaymentRequest payment3 =
                new RecordPaymentRequest(
                        totalAmount.multiply(new BigDecimal("0.3")).getAmount(),
                        LocalDate.now(),
                        PaymentMethod.CASH,
                        "REF-3");

        // When & Then
        mockMvc.perform(
                        post("/api/invoices/" + invoice.getId() + "/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payment3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.updatedInvoiceStatus").value("PAID"));

        // Verify final state
        Invoice paidInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();

        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paidInvoice.getBalanceDue().getAmount())
                .isLessThanOrEqualTo(new BigDecimal("0.01")); // Allow for rounding
    }

    @Test
    @DisplayName("Should fail to record payment for draft invoice")
    void shouldFailToRecordPaymentForDraftInvoice() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice().buildWithLineItemsAndSave(); // DRAFT status

        RecordPaymentRequest request =
                new RecordPaymentRequest(
                        new BigDecimal("50.00"),
                        LocalDate.now(),
                        PaymentMethod.BANK_TRANSFER,
                        "REF-12345");

        // When & Then
        mockMvc.perform(
                        post("/api/invoices/" + invoice.getId() + "/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        org.hamcrest.Matchers.containsString(
                                                "Cannot apply payment to invoice with status: DRAFT")));
    }

    @Test
    @DisplayName("Should fail when payment exceeds balance")
    void shouldFailWhenPaymentExceedsBalance() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice().buildSentAndSave();

        Money excessiveAmount = invoice.getBalanceDue().add(Money.of(1.0));

        RecordPaymentRequest request =
                new RecordPaymentRequest(
                        excessiveAmount.getAmount(),
                        LocalDate.now(),
                        PaymentMethod.BANK_TRANSFER,
                        "REF-12345");

        // When & Then
        mockMvc.perform(
                        post("/api/invoices/" + invoice.getId() + "/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        org.hamcrest.Matchers.containsString(
                                                "exceeds invoice balance")));
    }

    @Test
    @DisplayName("Should fail when payment amount is zero")
    void shouldFailWhenPaymentAmountIsZero() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice().buildSentAndSave();

        RecordPaymentRequest request =
                new RecordPaymentRequest(
                        BigDecimal.ZERO, LocalDate.now(), PaymentMethod.BANK_TRANSFER, "REF-12345");

        // When & Then
        mockMvc.perform(
                        post("/api/invoices/" + invoice.getId() + "/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    @DisplayName("Should fail when payment amount is negative")
    void shouldFailWhenPaymentAmountIsNegative() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice().buildSentAndSave();

        RecordPaymentRequest request =
                new RecordPaymentRequest(
                        new BigDecimal("-50.00"),
                        LocalDate.now(),
                        PaymentMethod.BANK_TRANSFER,
                        "REF-12345");

        // When & Then
        mockMvc.perform(
                        post("/api/invoices/" + invoice.getId() + "/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    @DisplayName("Should fail when payment date is in future")
    void shouldFailWhenPaymentDateIsInFuture() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice().buildSentAndSave();

        LocalDate futureDate = LocalDate.now().plusDays(1);

        RecordPaymentRequest request =
                new RecordPaymentRequest(
                        new BigDecimal("50.00"),
                        futureDate,
                        PaymentMethod.BANK_TRANSFER,
                        "REF-12345");

        // When & Then
        mockMvc.perform(
                        post("/api/invoices/" + invoice.getId() + "/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    @DisplayName("Should get payment by ID")
    void shouldGetPaymentById() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice().buildSentAndSave();

        Payment payment =
                testDataBuilder
                        .payment()
                        .invoice(invoice)
                        .amount(50.0)
                        .paymentMethod(PaymentMethod.BANK_TRANSFER)
                        .referenceNumber("REF-TEST")
                        .buildAndSave();

        // Apply payment to invoice
        invoice.applyPayment(payment.getAmount());
        invoiceRepository.save(invoice);

        // When & Then
        mockMvc.perform(get("/api/payments/" + payment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId().toString()))
                .andExpect(jsonPath("$.invoiceId").value(invoice.getId().toString()))
                .andExpect(jsonPath("$.amount").value(50.0))
                .andExpect(jsonPath("$.paymentMethod").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.referenceNumber").value("REF-TEST"));
    }

    @Test
    @DisplayName("Should list payments for invoice")
    void shouldListPaymentsForInvoice() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice().buildSentAndSave();

        Money totalAmount = invoice.getTotalAmount();
        Money firstPayment = totalAmount.multiply(new BigDecimal("0.3"));
        Money secondPayment = totalAmount.multiply(new BigDecimal("0.4"));

        // Create first payment
        Payment payment1 =
                testDataBuilder.payment().invoice(invoice).amount(firstPayment).buildAndSave();

        invoice.applyPayment(firstPayment);
        invoiceRepository.save(invoice);

        // Create second payment
        Payment payment2 =
                testDataBuilder.payment().invoice(invoice).amount(secondPayment).buildAndSave();

        invoice.applyPayment(secondPayment);
        invoiceRepository.save(invoice);

        // When & Then
        mockMvc.perform(get("/api/invoices/" + invoice.getId() + "/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].invoiceId").value(invoice.getId().toString()))
                .andExpect(jsonPath("$[1].invoiceId").value(invoice.getId().toString()));
    }

    @Test
    @DisplayName("Should accept different payment methods")
    void shouldAcceptDifferentPaymentMethods() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice().buildSentAndSave();

        Money quarterAmount = invoice.getTotalAmount().multiply(new BigDecimal("0.25"));

        // Test each payment method
        PaymentMethod[] methods = {
            PaymentMethod.BANK_TRANSFER,
            PaymentMethod.CREDIT_CARD,
            PaymentMethod.CASH,
            PaymentMethod.CHECK
        };

        for (PaymentMethod method : methods) {
            RecordPaymentRequest request =
                    new RecordPaymentRequest(
                            quarterAmount.getAmount(),
                            LocalDate.now(),
                            method,
                            "REF-" + method.name());

            // When & Then
            mockMvc.perform(
                            post("/api/invoices/" + invoice.getId() + "/payments")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists());
        }

        // Verify all payments were recorded
        List<Payment> payments = paymentRepository.findByInvoiceId(invoice.getId());
        assertThat(payments).hasSize(4);
        assertThat(payments)
                .extracting(Payment::getPaymentMethod)
                .containsExactlyInAnyOrder(methods);
    }
}
