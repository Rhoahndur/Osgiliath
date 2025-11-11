package com.osgiliath.application.customer.query;

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
 * Handler for GetCustomerByIdQuery
 * Retrieves a single customer by ID
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetCustomerByIdQueryHandler {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional(readOnly = true)
    public CustomerResponse handle(GetCustomerByIdQuery query) {
        log.debug("Fetching customer with ID: {}", query.getId());

        Customer customer = customerRepository.findById(query.getId())
                .orElseThrow(() -> new DomainException("Customer not found with ID: " + query.getId()));

        return customerMapper.toResponse(customer);
    }
}
