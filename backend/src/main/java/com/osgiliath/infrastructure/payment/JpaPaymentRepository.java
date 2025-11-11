package com.osgiliath.infrastructure.payment;

import com.osgiliath.domain.payment.Payment;
import com.osgiliath.domain.payment.PaymentRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of PaymentRepository
 * Uses Spring Data JPA for persistence operations
 */
@Repository
public interface JpaPaymentRepository extends JpaRepository<Payment, UUID>, PaymentRepository {

    @Override
    Payment save(Payment payment);

    @Override
    Optional<Payment> findById(UUID id);

    @Override
    List<Payment> findByInvoiceId(UUID invoiceId);

    @Override
    void delete(Payment payment);
}
