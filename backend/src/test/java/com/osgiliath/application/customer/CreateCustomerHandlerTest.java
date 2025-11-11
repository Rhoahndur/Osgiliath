package com.osgiliath.application.customer;

import com.osgiliath.application.customer.command.CreateCustomerCommand;
import com.osgiliath.application.customer.command.CreateCustomerHandler;
import com.osgiliath.application.customer.dto.CustomerMapper;
import com.osgiliath.application.customer.dto.CustomerResponse;
import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.customer.CustomerRepository;
import com.osgiliath.domain.shared.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateCustomerHandler
 * Uses mocks to isolate handler logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCustomerHandler")
class CreateCustomerHandlerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CreateCustomerHandler handler;

    private CreateCustomerCommand command;

    @BeforeEach
    void setUp() {
        command = new CreateCustomerCommand(
                "John Doe",
                "john.doe@example.com",
                "555-1234",
                "123 Main St"
        );
    }

    @Test
    @DisplayName("Should create customer successfully")
    void shouldCreateCustomerSuccessfully() {
        // Given
        Customer savedCustomer = Customer.create(
                command.getName(),
                command.getEmail(),
                command.getPhone(),
                command.getAddress()
        );
        CustomerResponse expectedResponse = new CustomerResponse();

        when(customerRepository.existsByEmail(command.getEmail())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(customerMapper.toResponse(savedCustomer)).thenReturn(expectedResponse);

        // When
        CustomerResponse result = handler.handle(command);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(customerRepository).existsByEmail(command.getEmail());
        verify(customerRepository).save(any(Customer.class));
        verify(customerMapper).toResponse(savedCustomer);
    }

    @Test
    @DisplayName("Should check email uniqueness before creating")
    void shouldCheckEmailUniquenessBeforeCreating() {
        // Given
        when(customerRepository.existsByEmail(command.getEmail())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerMapper.toResponse(any(Customer.class))).thenReturn(new CustomerResponse());

        // When
        handler.handle(command);

        // Then
        verify(customerRepository).existsByEmail(command.getEmail());
    }

    @Test
    @DisplayName("Should fail when email already exists")
    void shouldFailWhenEmailAlreadyExists() {
        // Given
        when(customerRepository.existsByEmail(command.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Customer with email " + command.getEmail() + " already exists");

        verify(customerRepository).existsByEmail(command.getEmail());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should delegate validation to domain object")
    void shouldDelegateValidationToDomainObject() {
        // Given
        CreateCustomerCommand invalidCommand = new CreateCustomerCommand(
                "",  // Invalid empty name
                "john@example.com",
                null,
                null
        );

        when(customerRepository.existsByEmail(invalidCommand.getEmail())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> handler.handle(invalidCommand))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Customer name cannot be empty");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should save customer to repository")
    void shouldSaveCustomerToRepository() {
        // Given
        when(customerRepository.existsByEmail(command.getEmail())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerMapper.toResponse(any(Customer.class))).thenReturn(new CustomerResponse());

        // When
        handler.handle(command);

        // Then
        verify(customerRepository).save(argThat(customer ->
                customer.getName().equals(command.getName()) &&
                customer.getEmailAddress().equals(command.getEmail().toLowerCase()) &&
                customer.getPhone().equals(command.getPhone()) &&
                customer.getAddress().equals(command.getAddress())
        ));
    }

    @Test
    @DisplayName("Should map domain object to response")
    void shouldMapDomainObjectToResponse() {
        // Given
        Customer savedCustomer = Customer.create(
                command.getName(),
                command.getEmail(),
                command.getPhone(),
                command.getAddress()
        );
        CustomerResponse expectedResponse = new CustomerResponse();

        when(customerRepository.existsByEmail(command.getEmail())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(customerMapper.toResponse(savedCustomer)).thenReturn(expectedResponse);

        // When
        CustomerResponse result = handler.handle(command);

        // Then
        assertThat(result).isEqualTo(expectedResponse);
        verify(customerMapper).toResponse(savedCustomer);
    }

    @Test
    @DisplayName("Should handle email case insensitivity")
    void shouldHandleEmailCaseInsensitivity() {
        // Given
        CreateCustomerCommand uppercaseCommand = new CreateCustomerCommand(
                "John Doe",
                "JOHN.DOE@EXAMPLE.COM",
                null,
                null
        );

        when(customerRepository.existsByEmail(uppercaseCommand.getEmail())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerMapper.toResponse(any(Customer.class))).thenReturn(new CustomerResponse());

        // When
        handler.handle(uppercaseCommand);

        // Then
        verify(customerRepository).save(argThat(customer ->
                customer.getEmailAddress().equals("john.doe@example.com")
        ));
    }
}
