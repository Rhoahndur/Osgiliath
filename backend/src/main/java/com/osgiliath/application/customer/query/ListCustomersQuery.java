package com.osgiliath.application.customer.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Query to retrieve a paginated list of customers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListCustomersQuery {
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
    private String search; // Optional search term for name or email
}
