package com.osgiliath.infrastructure.scheduler;

import com.osgiliath.application.invoice.MarkOverdueInvoicesCommand;
import com.osgiliath.application.invoice.MarkOverdueInvoicesHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceScheduler {
    private final MarkOverdueInvoicesHandler markOverdueInvoicesHandler;

    // Run daily at 1 AM
    @Scheduled(cron = "0 0 1 * * *")
    public void markOverdueInvoices() {
        log.info("Running scheduled task: Mark overdue invoices");
        MarkOverdueInvoicesCommand command = new MarkOverdueInvoicesCommand();
        int count = markOverdueInvoicesHandler.handle(command);
        log.info("Scheduled task completed: {} invoices marked as OVERDUE", count);
    }
}
