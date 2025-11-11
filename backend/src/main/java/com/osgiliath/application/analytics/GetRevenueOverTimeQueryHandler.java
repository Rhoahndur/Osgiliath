package com.osgiliath.application.analytics;

import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.invoice.InvoiceStatus;
import com.osgiliath.infrastructure.invoice.InvoiceSpecifications;
import com.osgiliath.infrastructure.invoice.JpaInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler for GetRevenueOverTimeQuery
 * Returns monthly revenue aggregated from paid invoices
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRevenueOverTimeQueryHandler {

    private final JpaInvoiceRepository invoiceRepository;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public List<MonthlyRevenueDto> handle(GetRevenueOverTimeQuery query) {
        int monthsToShow = query.getMonths();

        // Calculate date range
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(monthsToShow - 1).withDayOfMonth(1);

        // Build specification for PAID invoices within date range
        Specification<Invoice> spec = InvoiceSpecifications.withFilters(
                InvoiceStatus.PAID,
                null,
                startDate,
                endDate
        );

        // Fetch all paid invoices within the date range
        List<Invoice> invoices = invoiceRepository.findAll(spec);

        // Group by month and sum revenue
        Map<String, BigDecimal> revenueByMonth = invoices.stream()
                .collect(Collectors.groupingBy(
                        (Invoice invoice) -> YearMonth.from(invoice.getIssueDate()).format(MONTH_FORMATTER),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                (Invoice invoice) -> invoice.getTotalAmount().getAmount(),
                                BigDecimal::add
                        )
                ));

        // Generate all months in range, filling in zeros for months with no revenue
        List<MonthlyRevenueDto> result = new ArrayList<>();
        YearMonth currentMonth = YearMonth.from(startDate);
        YearMonth lastMonth = YearMonth.from(endDate);

        while (!currentMonth.isAfter(lastMonth)) {
            String monthKey = currentMonth.format(MONTH_FORMATTER);
            BigDecimal revenue = revenueByMonth.getOrDefault(monthKey, BigDecimal.ZERO);
            result.add(new MonthlyRevenueDto(monthKey, revenue));
            currentMonth = currentMonth.plusMonths(1);
        }

        return result;
    }
}
