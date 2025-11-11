package com.osgiliath.domain.invoice;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Invoice Repository interface (Domain layer)
 * Implementation will be in infrastructure layer
 */
public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(UUID id);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    void delete(Invoice invoice);

    void deleteAll();

    boolean existsByInvoiceNumber(String invoiceNumber);

    boolean existsByCustomerId(UUID customerId);

    List<Invoice> findAll();

    /**
     * Find invoices by status where due date is before the specified date
     */
    List<Invoice> findByStatusAndDueDateBefore(InvoiceStatus status, LocalDate date);
}
