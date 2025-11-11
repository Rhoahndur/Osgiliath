package com.osgiliath.application.payment.dto;

import com.osgiliath.application.payment.command.RecordPaymentCommand;
import com.osgiliath.application.payment.command.RecordPaymentResult;
import com.osgiliath.domain.payment.Payment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper for Payment DTOs
 * Converts between domain objects and API DTOs
 */
@Component
public class PaymentMapper {

    /**
     * Map RecordPaymentRequest to RecordPaymentCommand
     */
    public RecordPaymentCommand toCommand(UUID invoiceId, RecordPaymentRequest request) {
        return new RecordPaymentCommand(
                invoiceId,
                request.getAmount(),
                request.getPaymentDate(),
                request.getPaymentMethod(),
                request.getReferenceNumber()
        );
    }

    /**
     * Map Payment domain object to PaymentResponse DTO
     */
    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getAmount().getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentMethod(),
                payment.getReferenceNumber(),
                payment.getCreatedAt(),
                null, // No invoice info in basic response
                null  // No invoice info in basic response
        );
    }

    /**
     * Map RecordPaymentResult to PaymentResponse with invoice update info
     */
    public PaymentResponse toResponse(RecordPaymentResult result, Payment payment) {
        return new PaymentResponse(
                result.getPaymentId(),
                result.getInvoiceId(),
                result.getPaymentAmount().getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentMethod(),
                payment.getReferenceNumber(),
                payment.getCreatedAt(),
                result.getUpdatedBalance().getAmount(),
                result.getUpdatedInvoiceStatus()
        );
    }
}
