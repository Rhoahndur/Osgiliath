package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceStatus;
import com.osgiliath.infrastructure.invoice.JpaInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handler for ListInvoicesQuery
 * Returns filtered list of invoices with pagination and sorting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListInvoicesQueryHandler {

    private final JpaInvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public List<Invoice> handle(ListInvoicesQuery query) {
        log.debug("Fetching invoices - page: {}, size: {}, sortBy: {}, sortDirection: {}",
                  query.getPage(), query.getSize(), query.getSortBy(), query.getSortDirection());

        // Set defaults
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 20;
        String sortBy = query.getSortBy() != null ? query.getSortBy() : "issueDate";
        String sortDirection = query.getSortDirection() != null ? query.getSortDirection() : "DESC";

        // Create sort configuration
        Sort sort = Sort.by(
                sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortBy
        );

        // Create pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // Build specification with filters
        Specification<Invoice> spec = buildSpecification(
                query.getStatus(),
                query.getCustomerId(),
                query.getFromDate(),
                query.getToDate()
        );

        // Fetch invoice IDs with filters and pagination
        Page<Invoice> invoicePage = invoiceRepository.findAll(spec, pageable);

        if (invoicePage.isEmpty()) {
            return List.of();
        }

        // Extract IDs
        List<UUID> invoiceIds = invoicePage.getContent().stream()
                .map(Invoice::getId)
                .collect(Collectors.toList());

        // Fetch full invoices with line items
        List<Invoice> invoices = invoiceRepository.findAllByIdWithLineItems(invoiceIds);

        // Sort invoices in memory to match the order from the page query
        return invoices.stream()
                .sorted((i1, i2) -> {
                    int idx1 = invoiceIds.indexOf(i1.getId());
                    int idx2 = invoiceIds.indexOf(i2.getId());
                    return Integer.compare(idx1, idx2);
                })
                .collect(Collectors.toList());
    }

    private Specification<Invoice> buildSpecification(InvoiceStatus status, UUID customerId, LocalDate fromDate, LocalDate toDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (customerId != null) {
                predicates.add(criteriaBuilder.equal(root.get("customerId"), customerId));
            }

            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("issueDate"), fromDate));
            }

            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("issueDate"), toDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
