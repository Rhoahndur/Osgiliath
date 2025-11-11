package com.osgiliath.application.customer.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Query to retrieve a customer by ID
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetCustomerByIdQuery {
    private UUID id;
}
