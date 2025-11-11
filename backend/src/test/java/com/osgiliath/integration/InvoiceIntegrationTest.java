package com.osgiliath.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osgiliath.BaseIntegrationTest;
import com.osgiliath.application.invoice.AddLineItemCommand;
import com.osgiliath.application.invoice.CreateInvoiceCommand;
import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceStatus;
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
 * Integration tests for Invoice API endpoints
 * Tests invoice lifecycle: create -> add line items -> send -> paid
 */
@DisplayName("Invoice Integration Tests")
class InvoiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should create draft invoice via API")
    void shouldCreateDraftInvoiceViaApi() throws Exception {
        // Given
        Customer customer = testDataBuilder.customer().buildAndSave();

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                customer.getId(),
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                List.of()
        );

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(customer.getId().toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.subtotal").value(0.00))
                .andExpect(jsonPath("$.totalAmount").value(0.00))
                .andReturn();

        // Verify invoice was saved to database
        String responseBody = result.getResponse().getContentAsString();
        String invoiceId = objectMapper.readTree(responseBody).get("id").asText();

        Invoice savedInvoice = invoiceRepository.findById(UUID.fromString(invoiceId))
                .orElseThrow();

        assertThat(savedInvoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(savedInvoice.getInvoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    @DisplayName("Should add line item to draft invoice")
    void shouldAddLineItemToDraftInvoice() throws Exception {
        // Given
        Customer customer = testDataBuilder.customer().buildAndSave();
        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildAndSave();

        AddLineItemCommand command = new AddLineItemCommand(
                invoice.getId(),
                "Service A",
                "2",
                "100.00"
        );

        // When & Then
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems.length()").value(1))
                .andExpect(jsonPath("$.lineItems[0].description").value("Service A"))
                .andExpect(jsonPath("$.lineItems[0].quantity").value(2))
                .andExpect(jsonPath("$.lineItems[0].unitPrice").value(100.00))
                .andExpect(jsonPath("$.subtotal").value(200.00))
                .andExpect(jsonPath("$.taxAmount").value(20.00))
                .andExpect(jsonPath("$.totalAmount").value(220.00));

        // Verify in database
        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId())
                .orElseThrow();

        assertThat(updatedInvoice.getLineItems()).hasSize(1);
        assertThat(updatedInvoice.getSubtotal().getAmount()).isEqualByComparingTo("200.00");
        assertThat(updatedInvoice.getTotalAmount().getAmount()).isEqualByComparingTo("220.00");
    }

    @Test
    @DisplayName("Should calculate totals correctly with multiple line items")
    void shouldCalculateTotalsCorrectlyWithMultipleLineItems() throws Exception {
        // Given
        Customer customer = testDataBuilder.customer().buildAndSave();
        Invoice invoice = testDataBuilder.invoice()
                .customer(customer)
                .buildAndSave();

        // Add first line item: 2 x $100 = $200
        AddLineItemCommand command1 = new AddLineItemCommand(
                invoice.getId(),
                "Service A",
                "2",
                "100.00"
        );

        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command1)))
                .andExpect(status().isOk());

        // Add second line item: 1 x $50 = $50
        AddLineItemCommand command2 = new AddLineItemCommand(
                invoice.getId(),
                "Service B",
                "1",
                "50.00"
        );

        // When & Then
        // Subtotal = $250, Tax = $25, Total = $275
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems.length()").value(2))
                .andExpect(jsonPath("$.subtotal").value(250.00))
                .andExpect(jsonPath("$.taxAmount").value(25.00))
                .andExpect(jsonPath("$.totalAmount").value(275.00));
    }

    @Test
    @DisplayName("Should not add line item to sent invoice")
    void shouldNotAddLineItemToSentInvoice() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice()
                .buildSentAndSave();

        AddLineItemCommand command = new AddLineItemCommand(
                invoice.getId(),
                "Service A",
                "1",
                "100.00"
        );

        // When & Then
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/line-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot add line items to a non-draft invoice")));
    }

    @Test
    @DisplayName("Should send invoice with line items")
    void shouldSendInvoiceWithLineItems() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice()
                .buildWithLineItemsAndSave();

        // When & Then
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/send"))
                .andExpect(status().isOk());

        // Verify status changed to SENT and balance was set
        Invoice sentInvoice = invoiceRepository.findById(invoice.getId())
                .orElseThrow();

        assertThat(sentInvoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(sentInvoice.getBalanceDue()).isEqualTo(sentInvoice.getTotalAmount());
    }

    @Test
    @DisplayName("Should not send invoice without line items")
    void shouldNotSendInvoiceWithoutLineItems() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice().buildAndSave();

        // When & Then
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/send"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cannot send an invoice without line items")));

        // Verify status remains DRAFT
        Invoice unchangedInvoice = invoiceRepository.findById(invoice.getId())
                .orElseThrow();

        assertThat(unchangedInvoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
    }

    @Test
    @DisplayName("Should not send already sent invoice")
    void shouldNotSendAlreadySentInvoice() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice()
                .buildSentAndSave();

        // When & Then
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/send"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Only draft invoices can be sent")));
    }

    @Test
    @DisplayName("Should get invoice by ID")
    void shouldGetInvoiceById() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice()
                .invoiceNumber("INV-TEST-001")
                .buildAndSave();

        // When & Then
        mockMvc.perform(get("/api/invoices/" + invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoice.getId().toString()))
                .andExpect(jsonPath("$.invoiceNumber").value("INV-TEST-001"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("Should list all invoices")
    void shouldListAllInvoices() throws Exception {
        // Given
        Customer customer = testDataBuilder.customer().buildAndSave();
        testDataBuilder.invoice().customer(customer).invoiceNumber("INV-001").buildAndSave();
        testDataBuilder.invoice().customer(customer).invoiceNumber("INV-002").buildAndSave();
        testDataBuilder.invoice().customer(customer).invoiceNumber("INV-003").buildAndSave();

        // When & Then
        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("Should remove line item from draft invoice")
    void shouldRemoveLineItemFromDraftInvoice() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice()
                .buildWithLineItemsAndSave();

        UUID lineItemId = invoice.getLineItems().get(0).getId();

        // When & Then
        mockMvc.perform(delete("/api/invoices/" + invoice.getId() + "/line-items/" + lineItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineItems.length()").value(1));

        // Verify in database
        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId())
                .orElseThrow();

        assertThat(updatedInvoice.getLineItems()).hasSize(1);
    }

    @Test
    @DisplayName("Should get invoice balance")
    void shouldGetInvoiceBalance() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice()
                .buildSentAndSave();

        // When & Then
        mockMvc.perform(get("/api/invoices/" + invoice.getId() + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").exists())
                .andExpect(jsonPath("$.balanceDue").exists())
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("Should update invoice dates when in draft")
    void shouldUpdateInvoiceDatesWhenInDraft() throws Exception {
        // Given
        Invoice invoice = testDataBuilder.invoice()
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .buildAndSave();

        LocalDate newIssueDate = LocalDate.now().plusDays(1);
        LocalDate newDueDate = LocalDate.now().plusDays(45);

        String updateJson = String.format(
                "{\"issueDate\":\"%s\",\"dueDate\":\"%s\"}",
                newIssueDate,
                newDueDate
        );

        // When & Then
        mockMvc.perform(put("/api/invoices/" + invoice.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueDate").value(newIssueDate.toString()))
                .andExpect(jsonPath("$.dueDate").value(newDueDate.toString()));

        // Verify in database
        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId())
                .orElseThrow();

        assertThat(updatedInvoice.getIssueDate()).isEqualTo(newIssueDate);
        assertThat(updatedInvoice.getDueDate()).isEqualTo(newDueDate);
    }

    @Test
    @DisplayName("Should fail to create invoice with invalid dates")
    void shouldFailToCreateInvoiceWithInvalidDates() throws Exception {
        // Given
        Customer customer = testDataBuilder.customer().buildAndSave();

        CreateInvoiceCommand command = new CreateInvoiceCommand(
                customer.getId(),
                LocalDate.now(),
                LocalDate.now().minusDays(1),  // Due date before issue date
                List.of()
        );

        // When & Then
        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Due date cannot be before issue date")));
    }
}
