package com.osgiliath.domain.customer;

import com.osgiliath.domain.shared.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Customer aggregate
 * Tests business rules and invariants
 */
@DisplayName("Customer Aggregate")
class CustomerTest {

    @Test
    @DisplayName("Should create customer with valid data")
    void shouldCreateCustomerWithValidData() {
        Customer customer = Customer.create(
                "John Doe",
                "john.doe@example.com",
                "555-1234",
                "123 Main St"
        );

        assertThat(customer.getName()).isEqualTo("John Doe");
        assertThat(customer.getEmailAddress()).isEqualTo("john.doe@example.com");
        assertThat(customer.getPhone()).isEqualTo("555-1234");
        assertThat(customer.getAddress()).isEqualTo("123 Main St");
    }

    @Test
    @DisplayName("Should create customer with minimal data")
    void shouldCreateCustomerWithMinimalData() {
        Customer customer = Customer.create(
                "John Doe",
                "john.doe@example.com",
                null,
                null
        );

        assertThat(customer.getName()).isEqualTo("John Doe");
        assertThat(customer.getEmailAddress()).isEqualTo("john.doe@example.com");
        assertThat(customer.getPhone()).isNull();
        assertThat(customer.getAddress()).isNull();
    }

    @Test
    @DisplayName("Should normalize email to lowercase")
    void shouldNormalizeEmailToLowercase() {
        Customer customer = Customer.create(
                "John Doe",
                "JOHN.DOE@EXAMPLE.COM",
                null,
                null
        );

        assertThat(customer.getEmailAddress()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should fail when name is null")
    void shouldFailWhenNameIsNull() {
        assertThatThrownBy(() -> Customer.create(
                null,
                "john@example.com",
                null,
                null
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Customer name cannot be empty");
    }

    @Test
    @DisplayName("Should fail when name is blank")
    void shouldFailWhenNameIsBlank() {
        assertThatThrownBy(() -> Customer.create(
                "   ",
                "john@example.com",
                null,
                null
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Customer name cannot be empty");
    }

    @Test
    @DisplayName("Should fail when name is empty")
    void shouldFailWhenNameIsEmpty() {
        assertThatThrownBy(() -> Customer.create(
                "",
                "john@example.com",
                null,
                null
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Customer name cannot be empty");
    }

    @Test
    @DisplayName("Should fail when name exceeds 200 characters")
    void shouldFailWhenNameExceedsMaxLength() {
        String longName = "a".repeat(201);

        assertThatThrownBy(() -> Customer.create(
                longName,
                "john@example.com",
                null,
                null
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Customer name cannot exceed 200 characters");
    }

    @Test
    @DisplayName("Should fail when email is null")
    void shouldFailWhenEmailIsNull() {
        assertThatThrownBy(() -> Customer.create(
                "John Doe",
                null,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email address cannot be null or empty");
    }

    @Test
    @DisplayName("Should fail when email is blank")
    void shouldFailWhenEmailIsBlank() {
        assertThatThrownBy(() -> Customer.create(
                "John Doe",
                "   ",
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email address cannot be null or empty");
    }

    @Test
    @DisplayName("Should fail when email format is invalid")
    void shouldFailWhenEmailFormatIsInvalid() {
        assertThatThrownBy(() -> Customer.create(
                "John Doe",
                "not-an-email",
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email address format");
    }

    @Test
    @DisplayName("Should fail when email missing @ symbol")
    void shouldFailWhenEmailMissingAtSymbol() {
        assertThatThrownBy(() -> Customer.create(
                "John Doe",
                "john.example.com",
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email address format");
    }

    @Test
    @DisplayName("Should fail when email missing domain")
    void shouldFailWhenEmailMissingDomain() {
        assertThatThrownBy(() -> Customer.create(
                "John Doe",
                "john@",
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email address format");
    }

    @Test
    @DisplayName("Should update customer information")
    void shouldUpdateCustomerInformation() {
        Customer customer = Customer.create(
                "John Doe",
                "john.doe@example.com",
                "555-1234",
                "123 Main St"
        );

        customer.update(
                "Jane Doe",
                "jane.doe@example.com",
                "555-5678",
                "456 Oak Ave"
        );

        assertThat(customer.getName()).isEqualTo("Jane Doe");
        assertThat(customer.getEmailAddress()).isEqualTo("jane.doe@example.com");
        assertThat(customer.getPhone()).isEqualTo("555-5678");
        assertThat(customer.getAddress()).isEqualTo("456 Oak Ave");
    }

    @Test
    @DisplayName("Should validate name when updating")
    void shouldValidateNameWhenUpdating() {
        Customer customer = Customer.create(
                "John Doe",
                "john.doe@example.com",
                null,
                null
        );

        assertThatThrownBy(() -> customer.update(
                "",
                "john.doe@example.com",
                null,
                null
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Customer name cannot be empty");
    }

    @Test
    @DisplayName("Should validate email when updating")
    void shouldValidateEmailWhenUpdating() {
        Customer customer = Customer.create(
                "John Doe",
                "john.doe@example.com",
                null,
                null
        );

        assertThatThrownBy(() -> customer.update(
                "John Doe",
                "invalid-email",
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email address format");
    }
}
