package com.osgiliath.api.analytics;

import com.osgiliath.application.analytics.*;
import com.osgiliath.domain.invoice.InvoiceStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for analytics and reporting endpoints
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics and reporting endpoints for business intelligence")
public class AnalyticsController {

    private final GetRevenueOverTimeQueryHandler revenueOverTimeHandler;
    private final GetInvoiceStatusBreakdownQueryHandler statusBreakdownHandler;
    private final GetTopCustomersQueryHandler topCustomersHandler;

    /**
     * Get revenue over time
     * Returns monthly revenue aggregated from paid invoices
     *
     * @param months Number of months to include (default: 12)
     * @return List of monthly revenue data
     */
    @GetMapping("/revenue-over-time")
    @Operation(
            summary = "Get revenue over time",
            description = "Returns monthly revenue for the specified period. Only includes paid invoices."
    )
    public ResponseEntity<List<MonthlyRevenueDto>> getRevenueOverTime(
            @Parameter(description = "Number of months to include", example = "12")
            @RequestParam(defaultValue = "12") Integer months
    ) {
        GetRevenueOverTimeQuery query = new GetRevenueOverTimeQuery(months);
        List<MonthlyRevenueDto> result = revenueOverTimeHandler.handle(query);
        return ResponseEntity.ok(result);
    }

    /**
     * Get invoice status breakdown
     * Returns count of invoices grouped by status
     *
     * @return Map of invoice status to count
     */
    @GetMapping("/status-breakdown")
    @Operation(
            summary = "Get invoice status breakdown",
            description = "Returns the count of invoices grouped by status (DRAFT, SENT, PAID, OVERDUE, CANCELLED)"
    )
    public ResponseEntity<Map<InvoiceStatus, Long>> getStatusBreakdown() {
        GetInvoiceStatusBreakdownQuery query = new GetInvoiceStatusBreakdownQuery();
        Map<InvoiceStatus, Long> result = statusBreakdownHandler.handle(query);
        return ResponseEntity.ok(result);
    }

    /**
     * Get top customers by revenue
     * Returns top customers ranked by total revenue from paid invoices
     *
     * @param limit Number of top customers to return (default: 10)
     * @return List of top customers with revenue metrics
     */
    @GetMapping("/top-customers")
    @Operation(
            summary = "Get top customers by revenue",
            description = "Returns top customers ranked by total revenue from paid invoices, including invoice count"
    )
    public ResponseEntity<List<TopCustomerDto>> getTopCustomers(
            @Parameter(description = "Number of top customers to return", example = "10")
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        GetTopCustomersQuery query = new GetTopCustomersQuery(limit);
        List<TopCustomerDto> result = topCustomersHandler.handle(query);
        return ResponseEntity.ok(result);
    }
}
