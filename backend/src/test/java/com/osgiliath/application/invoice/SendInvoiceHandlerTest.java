package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.invoice.InvoiceStatus;
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
 * Unit tests for SendInvoiceHandler
 * Tests state transition logic from DRAFT to SENT
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SendInvoiceHandler")
class SendInvoiceHandlerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private SendInvoiceHandler handler;

    private UUID invoiceId;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        invoice = Invoice.create(
                customerId,
                "INV-001",
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );
        // Add line items so invoice can be sent
        invoice.addLineItem("Service A", BigDecimal.valueOf(1), Money.of(100.0));
    }

    @Test
    @DisplayName("Should send draft invoice successfully")
    void shouldSendDraftInvoiceSuccessfully() {
        // Given
        SendInvoiceCommand command = new SendInvoiceCommand(invoiceId);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // When
        handler.handle(command);

        // Then
        verify(invoiceRepository).findById(invoiceId);
        verify(invoiceRepository).save(argThat(inv ->
                inv.getStatus() == InvoiceStatus.SENT &&
                inv.getBalanceDue().equals(inv.getTotalAmount())
        ));
    }

    @Test
    @DisplayName("Should fail when invoice not found")
    void shouldFailWhenInvoiceNotFound() {
        // Given
        SendInvoiceCommand command = new SendInvoiceCommand(invoiceId);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Invoice not found: " + invoiceId);

        verify(invoiceRepository).findById(invoiceId);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should fail when invoice has no line items")
    void shouldFailWhenInvoiceHasNoLineItems() {
        // Given
        UUID customerId = UUID.randomUUID();
        Invoice emptyInvoice = Invoice.create(
                customerId,
                "INV-002",
                LocalDate.now(),
                LocalDate.now().plusDays(30)
        );
        SendInvoiceCommand command = new SendInvoiceCommand(invoiceId);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(emptyInvoice));

        // When & Then
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Cannot send an invoice without line items");

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should fail when invoice is already sent")
    void shouldFailWhenInvoiceIsAlreadySent() {
        // Given
        invoice.send(); // Already sent
        SendInvoiceCommand command = new SendInvoiceCommand(invoiceId);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // When & Then
        assertThatThrownBy(() -> handler.handle(command))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Only draft invoices can be sent");

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should set balance due equal to total amount")
    void shouldSetBalanceDueEqualToTotalAmount() {
        // Given
        SendInvoiceCommand command = new SendInvoiceCommand(invoiceId);
        Money expectedTotal = invoice.getTotalAmount();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // When
        handler.handle(command);

        // Then
        verify(invoiceRepository).save(argThat(inv ->
                inv.getBalanceDue().equals(expectedTotal)
        ));
    }

    @Test
    @DisplayName("Should transition status from DRAFT to SENT")
    void shouldTransitionStatusFromDraftToSent() {
        // Given
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);

        SendInvoiceCommand command = new SendInvoiceCommand(invoiceId);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // When
        handler.handle(command);

        // Then
        verify(invoiceRepository).save(argThat(inv ->
                inv.getStatus() == InvoiceStatus.SENT
        ));
    }

    @Test
    @DisplayName("Should save invoice after state transition")
    void shouldSaveInvoiceAfterStateTransition() {
        // Given
        SendInvoiceCommand command = new SendInvoiceCommand(invoiceId);
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // When
        handler.handle(command);

        // Then
        verify(invoiceRepository).save(invoice);
    }
}
