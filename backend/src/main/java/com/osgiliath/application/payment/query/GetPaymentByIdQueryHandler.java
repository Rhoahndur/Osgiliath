package com.osgiliath.application.payment.query;

import com.osgiliath.domain.payment.Payment;
import com.osgiliath.domain.payment.PaymentRepository;
import com.osgiliath.domain.shared.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for GetPaymentByIdQuery
 */
@Service
@RequiredArgsConstructor
public class GetPaymentByIdQueryHandler {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Payment handle(GetPaymentByIdQuery query) {
        return paymentRepository.findById(query.getPaymentId())
                .orElseThrow(() -> new DomainException(
                        "Payment not found: " + query.getPaymentId()
                ));
    }
}
