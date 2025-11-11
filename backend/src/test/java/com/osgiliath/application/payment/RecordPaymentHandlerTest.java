package com.osgiliath.application.payment;

import com.osgiliath.application.payment.command.RecordPaymentCommand;
import com.osgiliath.application.payment.command.RecordPaymentHandler;
import com.osgiliath.application.payment.command.RecordPaymentResult;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.invoice.InvoiceStatus;
import com.osgiliath.domain.payment.Payment;
import com.osgiliath.domain.payment.PaymentMethod;
import com.osgiliath.domain.payment.PaymentRepository;
import com.osgiliath.domain.shared.DomainException;
import com.osgiliath.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecordPaymentHandler
 * Tests payment application logic and invoice balance updates
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecordPaymentHandler")
class RecordPaymentHandlerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private RecordPaymentHandler handler;

    private UUID invoiceId;
    private Invoice invoice;
    private RecordPaymentCommand command;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        // Create and send an invoice
        invoice = Invoice.create(
                customerId,
                "INV-001",
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );
        invoice.setId(invoiceId);  // Set the ID for the mock invoice
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
        invoice.send();

        command = new RecordPaymentCommand(
                invoiceId,
                new BigDecimal("50.00"),
                LocalDate.now(),
                PaymentMethod.BANK_TRANSFER,
                "REF-12345"
        );
    }

    @Test
    @DisplayName("Should record payment successfully")
    void shouldRecordPaymentSuccessfully() {
        // Given
        Payment savedPayment = Payment.create(
                invoiceId,
                command.getPaymentDate(),
                Money.of(command.getAmount()),
                command.getPaymentMethod(),
                command.getReferenceNumber()
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // When
        RecordPaymentResult result = handler.handle(command);

        // Then
        assertThat(result).isNotNull();
        verify(invoiceRepository).findById(invoiceId);
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should fail when invoice not found")
    void shouldFailWhenInvoiceNotFound() {
        // Given
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Invoice not found: " + invoiceId);

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should fail when invoice is DRAFT")
    void shouldFailWhenInvoiceIsDraft() {
        // Given
        UUID customerId = UUID.randomUUID();
        Invoice draftInvoice = Invoice.create(
                customerId,
                "INV-002",
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );
        draftInvoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
        // Don't send - keep in DRAFT status

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(draftInvoice));

        // When & Then
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot apply payment to invoice with status: DRAFT");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should fail when payment amount exceeds balance")
    void shouldFailWhenPaymentAmountExceedsBalance() {
        // Given
        Money balanceDue = invoice.getBalanceDue();
        BigDecimal excessiveAmount = balanceDue.getAmount().add(new BigDecimal("1.00"));

        RecordPaymentCommand excessiveCommand = new RecordPaymentCommand(
                invoiceId,
                excessiveAmount,
                LocalDate.now(),
                PaymentMethod.BANK_TRANSFER,
                "REF-12345"
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // When & Then
        assertThatThrownBy(() -> handler.handle(excessiveCommand))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Payment amount")
                .hasMessageContaining("exceeds invoice balance");

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should reduce invoice balance after payment")
    void shouldReduceInvoiceBalanceAfterPayment() {
        // Given
        Money originalBalance = invoice.getBalanceDue();
        Money paymentAmount = Money.of(command.getAmount());
        Money expectedBalance = originalBalance.subtract(paymentAmount);

        Payment savedPayment = Payment.create(
                invoiceId,
                command.getPaymentDate(),
                paymentAmount,
                command.getPaymentMethod(),
                command.getReferenceNumber()
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // When
        handler.handle(command);

        // Then
        verify(invoiceRepository).save(argThat(inv ->
                inv.getBalanceDue().equals(expectedBalance)
        ));
    }

    @Test
    @DisplayName("Should transition invoice to PAID when balance is zero")
    void shouldTransitionInvoiceToPaidWhenBalanceIsZero() {
        // Given
        Money totalAmount = invoice.getTotalAmount();
        RecordPaymentCommand fullPaymentCommand = new RecordPaymentCommand(
                invoiceId,
                totalAmount.getAmount(),
                LocalDate.now(),
                PaymentMethod.BANK_TRANSFER,
                "REF-12345"
        );

        Payment savedPayment = Payment.create(
                invoiceId,
                fullPaymentCommand.getPaymentDate(),
                totalAmount,
                fullPaymentCommand.getPaymentMethod(),
                fullPaymentCommand.getReferenceNumber()
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // When
        handler.handle(fullPaymentCommand);

        // Then
        verify(invoiceRepository).save(argThat(inv ->
                inv.getStatus() == InvoiceStatus.PAID &&
                inv.getBalanceDue().isZero()
        ));
    }

    @Test
    @DisplayName("Should keep invoice SENT status for partial payment")
    void shouldKeepInvoiceSentStatusForPartialPayment() {
        // Given
        Payment savedPayment = Payment.create(
                invoiceId,
                command.getPaymentDate(),
                Money.of(command.getAmount()),
                command.getPaymentMethod(),
                command.getReferenceNumber()
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // When
        handler.handle(command);

        // Then
        verify(invoiceRepository).save(argThat(inv ->
                inv.getStatus() == InvoiceStatus.SENT &&
                !inv.getBalanceDue().isZero()
        ));
    }

    @Test
    @DisplayName("Should create payment with correct details")
    void shouldCreatePaymentWithCorrectDetails() {
        // Given
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // When
        handler.handle(command);

        // Then
        verify(paymentRepository).save(argThat(payment ->
                payment.getInvoiceId().equals(invoiceId) &&
                payment.getAmount().equals(Money.of(command.getAmount())) &&
                payment.getPaymentDate().equals(command.getPaymentDate()) &&
                payment.getPaymentMethod() == command.getPaymentMethod() &&
                payment.getReferenceNumber().equals(command.getReferenceNumber())
        ));
    }

    @Test
    @DisplayName("Should save both payment and invoice atomically")
    void shouldSaveBothPaymentAndInvoiceAtomically() {
        // Given
        Payment savedPayment = Payment.create(
                invoiceId,
                command.getPaymentDate(),
                Money.of(command.getAmount()),
                command.getPaymentMethod(),
                command.getReferenceNumber()
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // When
        handler.handle(command);

        // Then
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should return result with payment and invoice details")
    void shouldReturnResultWithPaymentAndInvoiceDetails() {
        // Given
        Payment savedPayment = Payment.create(
                invoiceId,
                command.getPaymentDate(),
                Money.of(command.getAmount()),
                command.getPaymentMethod(),
                command.getReferenceNumber()
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // When
        RecordPaymentResult result = handler.handle(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(result.getPaymentAmount()).isEqualTo(Money.of(command.getAmount()));
    }
}
