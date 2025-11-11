package com.osgiliath.domain.invoice;

/**
 * Invoice Status Enum
 * Defines the lifecycle states of an invoice
 */
public enum InvoiceStatus {
    DRAFT,      // Invoice is being created, can be edited
    SENT,       // Invoice has been sent to customer, can receive payments
    PAID,       // Invoice is fully paid, balance is zero
    OVERDUE,    // Invoice is past due date and still unpaid
    CANCELLED   // Invoice has been cancelled
}
