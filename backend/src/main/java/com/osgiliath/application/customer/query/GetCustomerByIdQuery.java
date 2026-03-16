package com.osgiliath.application.customer.query;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Query to retrieve a customer by ID */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetCustomerByIdQuery {
    private UUID id;
}
