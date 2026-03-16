package com.osgiliath.application.payment.query;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Query to retrieve a payment by its ID */
@Getter
@AllArgsConstructor
public class GetPaymentByIdQuery {

    private final UUID paymentId;
}
