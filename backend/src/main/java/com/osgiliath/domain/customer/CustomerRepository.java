package com.osgiliath.domain.customer;

import java.util.Optional;
import java.util.UUID;

/**
 * Customer Repository interface (Domain layer)
 * Implementation will be in infrastructure layer
 */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(UUID id);

    Optional<Customer> findByEmail(String email);

    void delete(Customer customer);

    void deleteAll();

    boolean existsByEmail(String email);
}
