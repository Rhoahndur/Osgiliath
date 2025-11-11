package com.osgiliath.infrastructure.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.invoice.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of InvoiceRepository
 * Extends Spring Data JPA for standard operations and custom queries
 */
@Repository
public interface JpaInvoiceRepository extends InvoiceRepository, JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    @Override
    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.lineItems WHERE i.id = :id")
    Optional<Invoice> findById(@Param("id") UUID id);

    @Override
    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.lineItems WHERE i.invoiceNumber = :invoiceNumber")
    Optional<Invoice> findByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);

    /**
     * Find all invoices with optional filters and eager loading of line items
     */
    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lineItems " +
           "WHERE (:status IS NULL OR i.status = :status) " +
           "AND (:customerId IS NULL OR i.customerId = :customerId) " +
           "AND (:fromDate IS NULL OR i.issueDate >= :fromDate) " +
           "AND (:toDate IS NULL OR i.issueDate <= :toDate) " +
           "ORDER BY i.issueDate DESC, i.createdAt DESC")
    List<Invoice> findAllWithFilters(
            @Param("status") InvoiceStatus status,
            @Param("customerId") UUID customerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /**
     * Count invoices by status for a specific customer
     */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.customerId = :customerId AND i.status = :status")
    long countByCustomerIdAndStatus(@Param("customerId") UUID customerId, @Param("status") InvoiceStatus status);

    /**
     * Find invoices by customer ID with eager loading
     */
    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.lineItems WHERE i.customerId = :customerId ORDER BY i.issueDate DESC")
    List<Invoice> findByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Find all invoices with optional filters, pagination and sorting
     * Note: Using two-step process to avoid Hibernate pagination issues with fetch joins
     * Using native query with explicit casts to avoid PostgreSQL type inference issues
     */
    @Query(value = "SELECT i.id FROM invoices i " +
           "WHERE (CAST(:status AS VARCHAR) IS NULL OR i.status = CAST(:status AS VARCHAR)) " +
           "AND (CAST(:customerId AS VARCHAR) IS NULL OR i.customer_id = CAST(:customerId AS UUID)) " +
           "AND (CAST(:fromDate AS VARCHAR) IS NULL OR i.issue_date >= CAST(:fromDate AS DATE)) " +
           "AND (CAST(:toDate AS VARCHAR) IS NULL OR i.issue_date <= CAST(:toDate AS DATE))",
           nativeQuery = true)
    Page<UUID> findAllIdsByFilters(
            @Param("status") String status,
            @Param("customerId") String customerId,
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate,
            Pageable pageable
    );

    /**
     * Fetch invoices by IDs with line items
     */
    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lineItems WHERE i.id IN :ids")
    List<Invoice> findAllByIdWithLineItems(@Param("ids") List<UUID> ids);

    /**
     * Check if any invoices exist for a customer
     */
    boolean existsByCustomerId(UUID customerId);

    /**
     * Find invoices by status where due date is before the specified date
     */
    @Query("SELECT i FROM Invoice i WHERE i.status = :status AND i.dueDate < :date")
    List<Invoice> findByStatusAndDueDateBefore(@Param("status") InvoiceStatus status, @Param("date") LocalDate date);
}
