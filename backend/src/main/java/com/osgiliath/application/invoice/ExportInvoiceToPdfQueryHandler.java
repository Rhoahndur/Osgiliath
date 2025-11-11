package com.osgiliath.application.invoice;

import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.customer.CustomerRepository;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.invoice.LineItem;
import com.osgiliath.domain.shared.DomainException;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Handler for ExportInvoiceToPdfQuery
 * Generates a professional PDF document for an invoice
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportInvoiceToPdfQueryHandler {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final com.osgiliath.domain.payment.PaymentRepository paymentRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Transactional(readOnly = true)
    public byte[] handle(ExportInvoiceToPdfQuery query) {
        log.debug("Generating PDF for invoice: {}", query.getInvoiceId());

        // Fetch invoice with line items
        Invoice invoice = invoiceRepository.findById(query.getInvoiceId())
                .orElseThrow(() -> new DomainException("Invoice not found: " + query.getInvoiceId()));

        // Fetch customer details
        Customer customer = customerRepository.findById(invoice.getCustomerId())
                .orElseThrow(() -> new DomainException("Customer not found: " + invoice.getCustomerId()));

        try {
            return generatePdf(invoice, customer);
        } catch (Exception e) {
            log.error("Error generating PDF for invoice: {}", query.getInvoiceId(), e);
            throw new DomainException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private byte[] generatePdf(Invoice invoice, Customer customer) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Fetch payments for this invoice
        var payments = paymentRepository.findByInvoiceId(invoice.getId());

        // Add company header
        addHeader(document);

        // Add invoice details and customer info
        addInvoiceInfo(document, invoice, customer);

        // Add line items table
        addLineItemsTable(document, invoice);

        // Add payment history (if any payments exist)
        if (!payments.isEmpty()) {
            addPaymentHistory(document, payments);
        }

        // Add totals
        addTotals(document, invoice);

        // Add footer
        addFooter(document);

        document.close();
        return baos.toByteArray();
    }

    private void addHeader(Document document) {
        Paragraph header = new Paragraph("INVOICE")
                .setFontSize(28)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(header);

        Paragraph companyName = new Paragraph("Osgiliath ERP System")
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(companyName);
    }

    private void addInvoiceInfo(Document document, Invoice invoice, Customer customer) {
        // Create a 2-column table for invoice details and customer info
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Left column - Invoice details
        Cell invoiceDetailsCell = new Cell()
                .setBorder(null)
                .add(new Paragraph("Invoice Details").setBold().setFontSize(12))
                .add(new Paragraph("Invoice #: " + invoice.getInvoiceNumber()).setFontSize(10))
                .add(new Paragraph("Issue Date: " + invoice.getIssueDate().format(DATE_FORMATTER)).setFontSize(10))
                .add(new Paragraph("Due Date: " + invoice.getDueDate().format(DATE_FORMATTER)).setFontSize(10))
                .add(new Paragraph("Status: " + invoice.getStatus()).setFontSize(10).setBold());

        // Right column - Customer info
        Cell customerInfoCell = new Cell()
                .setBorder(null)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Bill To").setBold().setFontSize(12))
                .add(new Paragraph(customer.getName()).setFontSize(10))
                .add(new Paragraph(customer.getEmailAddress()).setFontSize(10));

        if (customer.getPhone() != null && !customer.getPhone().isBlank()) {
            customerInfoCell.add(new Paragraph(customer.getPhone()).setFontSize(10));
        }

        if (customer.getAddress() != null && !customer.getAddress().isBlank()) {
            customerInfoCell.add(new Paragraph(customer.getAddress()).setFontSize(10));
        }

        infoTable.addCell(invoiceDetailsCell);
        infoTable.addCell(customerInfoCell);
        document.add(infoTable);
    }

    private void addLineItemsTable(Document document, Invoice invoice) {
        // Create table with 4 columns: Description, Quantity, Unit Price, Line Total
        float[] columnWidths = {4f, 1f, 1.5f, 1.5f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Add header row
        table.addHeaderCell(createHeaderCell("Description"));
        table.addHeaderCell(createHeaderCell("Quantity"));
        table.addHeaderCell(createHeaderCell("Unit Price"));
        table.addHeaderCell(createHeaderCell("Amount"));

        // Add line items
        for (LineItem item : invoice.getLineItems()) {
            table.addCell(createCell(item.getDescription(), TextAlignment.LEFT));
            table.addCell(createCell(item.getQuantity().stripTrailingZeros().toPlainString(), TextAlignment.CENTER));
            table.addCell(createCell("$" + item.getUnitPrice().getAmount().toPlainString(), TextAlignment.RIGHT));
            table.addCell(createCell("$" + item.getLineTotal().getAmount().toPlainString(), TextAlignment.RIGHT));
        }

        document.add(table);
    }

    private void addPaymentHistory(Document document, java.util.List<com.osgiliath.domain.payment.Payment> payments) {
        // Add section header
        Paragraph paymentHeader = new Paragraph("Payment History")
                .setFontSize(12)
                .setBold()
                .setMarginBottom(10)
                .setMarginTop(10);
        document.add(paymentHeader);

        // Create table with 4 columns: Date, Method, Reference, Amount
        float[] columnWidths = {2f, 2f, 3f, 1.5f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Add header row
        table.addHeaderCell(createHeaderCell("Date"));
        table.addHeaderCell(createHeaderCell("Method"));
        table.addHeaderCell(createHeaderCell("Reference"));
        table.addHeaderCell(createHeaderCell("Amount"));

        // Add payments
        for (com.osgiliath.domain.payment.Payment payment : payments) {
            table.addCell(createCell(payment.getPaymentDate().format(DATE_FORMATTER), TextAlignment.LEFT));
            table.addCell(createCell(payment.getPaymentMethod().toString().replace("_", " "), TextAlignment.LEFT));
            table.addCell(createCell(payment.getReferenceNumber() != null ? payment.getReferenceNumber() : "-", TextAlignment.LEFT));
            table.addCell(createCell("$" + payment.getAmount().getAmount().toPlainString(), TextAlignment.RIGHT));
        }

        document.add(table);
    }

    private void addTotals(Document document, Invoice invoice) {
        // Create a table for totals aligned to the right
        Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Subtotal
        totalsTable.addCell(createTotalLabelCell("Subtotal:"));
        totalsTable.addCell(createTotalValueCell("$" + invoice.getSubtotal().getAmount().toPlainString()));

        // Tax
        totalsTable.addCell(createTotalLabelCell("Tax:"));
        totalsTable.addCell(createTotalValueCell("$" + invoice.getTaxAmount().getAmount().toPlainString()));

        // Total
        totalsTable.addCell(createTotalLabelCell("Total:"));
        totalsTable.addCell(createTotalValueCell("$" + invoice.getTotalAmount().getAmount().toPlainString()));

        // Balance Due (highlighted)
        Cell balanceLabel = createTotalLabelCell("Balance Due:");
        balanceLabel.setBold().setFontSize(12);

        Cell balanceValue = createTotalValueCell("$" + invoice.getBalanceDue().getAmount().toPlainString());
        balanceValue.setBold().setFontSize(12).setBackgroundColor(ColorConstants.LIGHT_GRAY);

        totalsTable.addCell(balanceLabel);
        totalsTable.addCell(balanceValue);

        document.add(totalsTable);
    }

    private void addFooter(Document document) {
        Paragraph footer = new Paragraph("Thank you for your business!")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic()
                .setMarginTop(30);
        document.add(footer);
    }

    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10);
    }

    private Cell createCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text))
                .setTextAlignment(alignment)
                .setFontSize(10);
    }

    private Cell createTotalLabelCell(String text) {
        return new Cell()
                .setBorder(null)
                .add(new Paragraph(text))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(10);
    }

    private Cell createTotalValueCell(String text) {
        return new Cell()
                .setBorder(null)
                .add(new Paragraph(text))
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(10);
    }
}
