package com.osgiliath.application.customer.query;

import com.osgiliath.application.customer.dto.CustomerMapper;
import com.osgiliath.application.customer.dto.CustomerResponse;
import com.osgiliath.domain.customer.Customer;
import com.osgiliath.infrastructure.customer.JpaCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for ListCustomersQuery
 * Retrieves a paginated list of customers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListCustomersQueryHandler {

    private final JpaCustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional(readOnly = true)
    public Page<CustomerResponse> handle(ListCustomersQuery query) {
        log.debug("Fetching customers - page: {}, size: {}, search: {}",
                  query.getPage(), query.getSize(), query.getSearch());

        // Create sort configuration
        Sort sort = Sort.by(
                query.getSortDirection().equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
                query.getSortBy()
        );

        // Create pageable
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), sort);

        // Fetch customers with or without search
        Page<Customer> customersPage;
        if (query.getSearch() != null && !query.getSearch().trim().isEmpty()) {
            customersPage = customerRepository.searchByNameOrEmail(query.getSearch(), pageable);
        } else {
            customersPage = customerRepository.findAll(pageable);
        }

        // Map to response DTOs
        return customersPage.map(customerMapper::toResponse);
    }
}
