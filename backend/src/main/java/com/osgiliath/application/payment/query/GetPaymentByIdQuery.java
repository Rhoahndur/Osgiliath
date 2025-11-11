package com.osgiliath.application.payment.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Query to retrieve a payment by its ID
 */
@Getter
@AllArgsConstructor
public class GetPaymentByIdQuery {

    private final UUID paymentId;
}
