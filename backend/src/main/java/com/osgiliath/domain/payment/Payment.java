package com.osgiliath.domain.payment;

import com.osgiliath.domain.shared.BaseEntity;
import com.osgiliath.domain.shared.DomainException;
import com.osgiliath.domain.shared.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Payment Aggregate Root
 * Represents a payment applied to an invoice
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_invoice", columnList = "invoice_id"),
        @Index(name = "idx_payment_date", columnList = "payment_date")
})
@Getter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false))
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    private Payment(UUID invoiceId, LocalDate paymentDate, Money amount,
                    PaymentMethod paymentMethod, String referenceNumber) {
        this.invoiceId = invoiceId;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
    }

    /**
     * Factory method to create a new payment
     */
    public static Payment create(UUID invoiceId, LocalDate paymentDate, Money amount,
                                  PaymentMethod paymentMethod, String referenceNumber) {
        validateInvoiceId(invoiceId);
        validatePaymentDate(paymentDate);
        validateAmount(amount);
        validatePaymentMethod(paymentMethod);

        return new Payment(invoiceId, paymentDate, amount, paymentMethod, referenceNumber);
    }

    private static void validateInvoiceId(UUID invoiceId) {
        if (invoiceId == null) {
            throw new DomainException("Invoice ID cannot be null");
        }
    }

    private static void validatePaymentDate(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new DomainException("Payment date cannot be null");
        }
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new DomainException("Payment date cannot be in the future");
        }
    }

    private static void validateAmount(Money amount) {
        if (amount == null || amount.isZero() || amount.isNegative()) {
            throw new DomainException("Payment amount must be greater than zero");
        }
    }

    private static void validatePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new DomainException("Payment method cannot be null");
        }
    }
}
