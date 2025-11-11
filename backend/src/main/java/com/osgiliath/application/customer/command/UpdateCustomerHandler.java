package com.osgiliath.application.customer.command;

import com.osgiliath.application.customer.dto.CustomerMapper;
import com.osgiliath.application.customer.dto.CustomerResponse;
import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.customer.CustomerRepository;
import com.osgiliath.domain.shared.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for UpdateCustomerCommand
 * Encapsulates business logic for customer updates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateCustomerHandler {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional
    public CustomerResponse handle(UpdateCustomerCommand command) {
        log.info("Updating customer with ID: {}", command.getId());

        // Find existing customer
        Customer customer = customerRepository.findById(command.getId())
                .orElseThrow(() -> new DomainException("Customer not found with ID: " + command.getId()));

        // Check if email is being changed to an existing email
        if (!customer.getEmailAddress().equals(command.getEmail())) {
            if (customerRepository.existsByEmail(command.getEmail())) {
                throw new DomainException("Customer with email " + command.getEmail() + " already exists");
            }
        }

        // Update customer using domain method
        customer.update(
                command.getName(),
                command.getEmail(),
                command.getPhone(),
                command.getAddress()
        );

        // Save updated customer
        Customer updatedCustomer = customerRepository.save(customer);

        log.info("Customer updated successfully with ID: {}", updatedCustomer.getId());

        return customerMapper.toResponse(updatedCustomer);
    }
}
