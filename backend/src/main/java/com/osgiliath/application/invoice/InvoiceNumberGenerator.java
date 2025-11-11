package com.osgiliath.application.invoice;

import com.osgiliath.domain.invoice.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service to generate unique invoice numbers
 * Format: INV-YYYYMMDD-XXXXX
 * Example: INV-20251107-00001
 */
@Service
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private static final String PREFIX = "INV-";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int SEQUENCE_LENGTH = 5;

    private final InvoiceRepository invoiceRepository;

    /**
     * Generate a unique invoice number for the current date
     */
    public String generate() {
        return generate(LocalDate.now());
    }

    /**
     * Generate a unique invoice number for a specific date
     */
    public String generate(LocalDate date) {
        String dateStr = date.format(DATE_FORMATTER);
        int sequence = 1;
        String invoiceNumber;

        // Keep trying until we find a unique number
        do {
            String sequenceStr = String.format("%0" + SEQUENCE_LENGTH + "d", sequence);
            invoiceNumber = PREFIX + dateStr + "-" + sequenceStr;
            sequence++;
        } while (invoiceRepository.existsByInvoiceNumber(invoiceNumber));

        return invoiceNumber;
    }
}
