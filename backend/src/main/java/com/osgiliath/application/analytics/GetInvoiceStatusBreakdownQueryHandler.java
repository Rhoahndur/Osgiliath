package com.osgiliath.application.analytics;

import com.osgiliath.domain.invoice.InvoiceStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for GetInvoiceStatusBreakdownQuery
 * Returns count of invoices grouped by status
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetInvoiceStatusBreakdownQueryHandler {

    @PersistenceContext
    private EntityManager entityManager;

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public Map<InvoiceStatus, Long> handle(GetInvoiceStatusBreakdownQuery query) {
        // Query to get count by status
        var results = entityManager.createQuery(
                "SELECT i.status, COUNT(i) FROM Invoice i GROUP BY i.status",
                Object[].class
        ).getResultList();

        Map<InvoiceStatus, Long> breakdown = new HashMap<>();

        // Initialize all statuses with 0
        for (InvoiceStatus status : InvoiceStatus.values()) {
            breakdown.put(status, 0L);
        }

        // Fill in actual counts
        for (Object[] result : results) {
            InvoiceStatus status = (InvoiceStatus) result[0];
            Long count = (Long) result[1];
            breakdown.put(status, count);
        }

        return breakdown;
    }
}
