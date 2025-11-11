package com.osgiliath.application.customer.command;

import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.customer.CustomerRepository;
import com.osgiliath.domain.exceptions.CustomerHasInvoicesException;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.shared.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for DeleteCustomerCommand
 * Encapsulates business logic for customer deletion
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteCustomerHandler {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void handle(DeleteCustomerCommand command) {
        log.info("Deleting customer with ID: {}", command.getId());

        // Find existing customer
        Customer customer = customerRepository.findById(command.getId())
                .orElseThrow(() -> new DomainException("Customer not found with ID: " + command.getId()));

        // Business rule: Cannot delete customer if they have any invoices
        if (invoiceRepository.existsByCustomerId(command.getId())) {
            throw new CustomerHasInvoicesException(
                    "Cannot delete customer with existing invoices. Please delete or cancel all invoices first."
            );
        }

        // Delete customer
        customerRepository.delete(customer);

        log.info("Customer deleted successfully with ID: {}", command.getId());
    }
}
