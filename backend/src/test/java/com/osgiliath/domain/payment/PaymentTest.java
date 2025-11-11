package com.osgiliath.domain.payment;

import com.osgiliath.domain.shared.DomainException;
import com.osgiliath.domain.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Payment aggregate
 * Tests payment validation and business rules
 */
@DisplayName("Payment Aggregate")
class PaymentTest {

    private UUID invoiceId;
    private LocalDate paymentDate;
    private Money amount;
    private PaymentMethod paymentMethod;
    private String referenceNumber;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        paymentDate = LocalDate.now();
        amount = Money.of(100.0);
        paymentMethod = PaymentMethod.BANK_TRANSFER;
        referenceNumber = "REF-12345";
    }

    @Test
    @DisplayName("Should create payment with valid data")
    void shouldCreatePaymentWithValidData() {
        Payment payment = Payment.create(
                invoiceId,
                paymentDate,
                amount,
                paymentMethod,
                referenceNumber
        );

        assertThat(payment.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(payment.getPaymentDate()).isEqualTo(paymentDate);
        assertThat(payment.getAmount()).isEqualTo(amount);
        assertThat(payment.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(payment.getReferenceNumber()).isEqualTo(referenceNumber);
    }

    @Test
    @DisplayName("Should create payment without reference number")
    void shouldCreatePaymentWithoutReferenceNumber() {
        Payment payment = Payment.create(
                invoiceId,
                paymentDate,
                amount,
                paymentMethod,
                null
        );

        assertThat(payment.getReferenceNumber()).isNull();
    }

    @Test
    @DisplayName("Should create payment with different payment methods")
    void shouldCreatePaymentWithDifferentPaymentMethods() {
        Payment bankTransfer = Payment.create(invoiceId, paymentDate, amount, PaymentMethod.BANK_TRANSFER, null);
        Payment creditCard = Payment.create(invoiceId, paymentDate, amount, PaymentMethod.CREDIT_CARD, null);
        Payment cash = Payment.create(invoiceId, paymentDate, amount, PaymentMethod.CASH, null);
        Payment check = Payment.create(invoiceId, paymentDate, amount, PaymentMethod.CHECK, null);

        assertThat(bankTransfer.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(creditCard.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(cash.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(check.getPaymentMethod()).isEqualTo(PaymentMethod.CHECK);
    }

    @Test
    @DisplayName("Should fail when invoice ID is null")
    void shouldFailWhenInvoiceIdIsNull() {
        assertThatThrownBy(() -> Payment.create(
                null,
                paymentDate,
                amount,
                paymentMethod,
                referenceNumber
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Invoice ID cannot be null");
    }

    @Test
    @DisplayName("Should fail when payment date is null")
    void shouldFailWhenPaymentDateIsNull() {
        assertThatThrownBy(() -> Payment.create(
                invoiceId,
                null,
                amount,
                paymentMethod,
                referenceNumber
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Payment date cannot be null");
    }

    @Test
    @DisplayName("Should fail when payment date is in the future")
    void shouldFailWhenPaymentDateIsInTheFuture() {
        LocalDate futureDate = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> Payment.create(
                invoiceId,
                futureDate,
                amount,
                paymentMethod,
                referenceNumber
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Payment date cannot be in the future");
    }

    @Test
    @DisplayName("Should allow payment date to be today")
    void shouldAllowPaymentDateToBeToday() {
        LocalDate today = LocalDate.now();

        Payment payment = Payment.create(
                invoiceId,
                today,
                amount,
                paymentMethod,
                referenceNumber
        );

        assertThat(payment.getPaymentDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("Should allow payment date in the past")
    void shouldAllowPaymentDateInThePast() {
        LocalDate pastDate = LocalDate.now().minusDays(10);

        Payment payment = Payment.create(
                invoiceId,
                pastDate,
                amount,
                paymentMethod,
                referenceNumber
        );

        assertThat(payment.getPaymentDate()).isEqualTo(pastDate);
    }

    @Test
    @DisplayName("Should fail when amount is null")
    void shouldFailWhenAmountIsNull() {
        assertThatThrownBy(() -> Payment.create(
                invoiceId,
                paymentDate,
                null,
                paymentMethod,
                referenceNumber
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Payment amount must be greater than zero");
    }

    @Test
    @DisplayName("Should fail when amount is zero")
    void shouldFailWhenAmountIsZero() {
        assertThatThrownBy(() -> Payment.create(
                invoiceId,
                paymentDate,
                Money.zero(),
                paymentMethod,
                referenceNumber
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Payment amount must be greater than zero");
    }

    @Test
    @DisplayName("Should fail when amount is negative")
    void shouldFailWhenAmountIsNegative() {
        Money negativeAmount = Money.of(-100.0);

        assertThatThrownBy(() -> Payment.create(
                invoiceId,
                paymentDate,
                negativeAmount,
                paymentMethod,
                referenceNumber
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Payment amount must be greater than zero");
    }

    @Test
    @DisplayName("Should accept small positive amounts")
    void shouldAcceptSmallPositiveAmounts() {
        Money smallAmount = Money.of(0.01);

        Payment payment = Payment.create(
                invoiceId,
                paymentDate,
                smallAmount,
                paymentMethod,
                referenceNumber
        );

        assertThat(payment.getAmount()).isEqualTo(smallAmount);
    }

    @Test
    @DisplayName("Should accept large amounts")
    void shouldAcceptLargeAmounts() {
        Money largeAmount = Money.of(1000000.0);

        Payment payment = Payment.create(
                invoiceId,
                paymentDate,
                largeAmount,
                paymentMethod,
                referenceNumber
        );

        assertThat(payment.getAmount()).isEqualTo(largeAmount);
    }

    @Test
    @DisplayName("Should fail when payment method is null")
    void shouldFailWhenPaymentMethodIsNull() {
        assertThatThrownBy(() -> Payment.create(
                invoiceId,
                paymentDate,
                amount,
                null,
                referenceNumber
        ))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Payment method cannot be null");
    }
}
