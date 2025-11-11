package com.osgiliath.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osgiliath.BaseIntegrationTest;
import com.osgiliath.application.customer.command.CreateCustomerCommand;
import com.osgiliath.application.customer.command.UpdateCustomerCommand;
import com.osgiliath.domain.customer.Customer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Customer API endpoints
 * Tests full CRUD operations with real database
 */
@DisplayName("Customer Integration Tests")
class CustomerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should create customer via API")
    void shouldCreateCustomerViaApi() throws Exception {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
                "John Doe",
                "john.doe@example.com",
                "555-1234",
                "123 Main St"
        );

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.phone").value("555-1234"))
                .andExpect(jsonPath("$.address").value("123 Main St"))
                .andReturn();

        // Verify customer was saved to database
        String responseBody = result.getResponse().getContentAsString();
        String customerId = objectMapper.readTree(responseBody).get("id").asText();

        Customer savedCustomer = customerRepository.findById(UUID.fromString(customerId))
                .orElseThrow();

        assertThat(savedCustomer.getName()).isEqualTo("John Doe");
        assertThat(savedCustomer.getEmailAddress()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should fail to create customer with duplicate email")
    void shouldFailToCreateCustomerWithDuplicateEmail() throws Exception {
        // Given
        Customer existingCustomer = testDataBuilder.customer()
                .email("duplicate@example.com")
                .buildAndSave();

        CreateCustomerCommand command = new CreateCustomerCommand(
                "Another Customer",
                "duplicate@example.com",
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    @DisplayName("Should fail to create customer with invalid email")
    void shouldFailToCreateCustomerWithInvalidEmail() throws Exception {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
                "John Doe",
                "not-an-email",
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid email")));
    }

    @Test
    @DisplayName("Should fail to create customer with empty name")
    void shouldFailToCreateCustomerWithEmptyName() throws Exception {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
                "",
                "john@example.com",
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name cannot be empty")));
    }

    @Test
    @DisplayName("Should get customer by ID")
    void shouldGetCustomerById() throws Exception {
        // Given
        Customer customer = testDataBuilder.customer()
                .name("John Doe")
                .email("john@example.com")
                .buildAndSave();

        // When & Then
        mockMvc.perform(get("/api/customers/" + customer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("Should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/api/customers/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should list all customers")
    void shouldListAllCustomers() throws Exception {
        // Given
        testDataBuilder.customer().name("Customer 1").email("customer1@example.com").buildAndSave();
        testDataBuilder.customer().name("Customer 2").email("customer2@example.com").buildAndSave();
        testDataBuilder.customer().name("Customer 3").email("customer3@example.com").buildAndSave();

        // When & Then
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[1].name").exists())
                .andExpect(jsonPath("$[2].name").exists());
    }

    @Test
    @DisplayName("Should update customer")
    void shouldUpdateCustomer() throws Exception {
        // Given
        Customer customer = testDataBuilder.customer()
                .name("John Doe")
                .email("john@example.com")
                .buildAndSave();

        UpdateCustomerCommand command = new UpdateCustomerCommand(
                customer.getId(),
                "Jane Doe",
                "jane@example.com",
                "555-9999",
                "456 Oak Ave"
        );

        // When & Then
        mockMvc.perform(put("/api/customers/" + customer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.phone").value("555-9999"))
                .andExpect(jsonPath("$.address").value("456 Oak Ave"));

        // Verify update in database
        Customer updatedCustomer = customerRepository.findById(customer.getId())
                .orElseThrow();

        assertThat(updatedCustomer.getName()).isEqualTo("Jane Doe");
        assertThat(updatedCustomer.getEmailAddress()).isEqualTo("jane@example.com");
    }

    @Test
    @DisplayName("Should delete customer")
    void shouldDeleteCustomer() throws Exception {
        // Given
        Customer customer = testDataBuilder.customer()
                .name("John Doe")
                .email("john@example.com")
                .buildAndSave();

        UUID customerId = customer.getId();

        // When & Then
        mockMvc.perform(delete("/api/customers/" + customerId))
                .andExpect(status().isNoContent());

        // Verify deletion
        assertThat(customerRepository.findById(customerId)).isEmpty();
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent customer")
    void shouldReturn404WhenDeletingNonExistentCustomer() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(delete("/api/customers/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should normalize email to lowercase when creating")
    void shouldNormalizeEmailToLowercaseWhenCreating() throws Exception {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
                "John Doe",
                "JOHN.DOE@EXAMPLE.COM",
                null,
                null
        );

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andReturn();

        // Verify in database
        String responseBody = result.getResponse().getContentAsString();
        String customerId = objectMapper.readTree(responseBody).get("id").asText();

        Customer savedCustomer = customerRepository.findById(UUID.fromString(customerId))
                .orElseThrow();

        assertThat(savedCustomer.getEmailAddress()).isEqualTo("john.doe@example.com");
    }
}
