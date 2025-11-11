package com.osgiliath.application.customer.dto;

import com.osgiliath.domain.customer.Customer;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Customer domain model and DTOs
 */
@Component
public class CustomerMapper {

    /**
     * Convert Customer entity to CustomerResponse DTO
     */
    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) {
            return null;
        }

        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmailAddress())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .version(customer.getVersion())
                .build();
    }
}
