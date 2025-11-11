package com.osgiliath.application.analytics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handler for GetTopCustomersQuery
 * Returns top customers ranked by total revenue from paid invoices
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetTopCustomersQueryHandler {

    @PersistenceContext
    private EntityManager entityManager;

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public List<TopCustomerDto> handle(GetTopCustomersQuery query) {
        int limit = query.getLimit();

        // Query to join invoices with customers and aggregate by customer
        var results = entityManager.createQuery(
                "SELECT i.customerId, c.name, SUM(i.totalAmount.amount), COUNT(i) " +
                "FROM Invoice i " +
                "JOIN Customer c ON i.customerId = c.id " +
                "WHERE i.status = com.osgiliath.domain.invoice.InvoiceStatus.PAID " +
                "GROUP BY i.customerId, c.name " +
                "ORDER BY SUM(i.totalAmount.amount) DESC",
                Object[].class
        )
        .setMaxResults(limit)
        .getResultList();

        return results.stream()
                .map(result -> new TopCustomerDto(
                        (UUID) result[0],           // customerId
                        (String) result[1],         // customerName
                        (BigDecimal) result[2],     // totalRevenue
                        (Long) result[3]            // invoiceCount
                ))
                .collect(Collectors.toList());
    }
}
