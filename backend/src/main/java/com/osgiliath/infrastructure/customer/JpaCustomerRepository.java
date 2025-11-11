package com.osgiliath.infrastructure.customer;

import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.customer.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of CustomerRepository
 * Extends both Spring Data JpaRepository and domain CustomerRepository
 */
@Repository
public interface JpaCustomerRepository extends JpaRepository<Customer, UUID>, CustomerRepository {

    @Override
    @Query("SELECT c FROM Customer c WHERE c.email.address = :email")
    Optional<Customer> findByEmail(@Param("email") String email);

    @Override
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Customer c WHERE c.email.address = :email")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Find all customers with pagination
     */
    Page<Customer> findAll(Pageable pageable);

    /**
     * Search customers by name or email with pagination
     * @param searchTerm Search term to match against name or email (case-insensitive)
     * @param pageable Pagination parameters
     * @return Page of matching customers
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.email.address) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Customer> searchByNameOrEmail(@Param("searchTerm") String searchTerm, Pageable pageable);
}
