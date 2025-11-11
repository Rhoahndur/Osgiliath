package com.osgiliath.domain.payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment Repository interface (Domain layer)
 * Implementation will be in infrastructure layer
 */
public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    List<Payment> findByInvoiceId(UUID invoiceId);

    void delete(Payment payment);

    void deleteAll();
}
