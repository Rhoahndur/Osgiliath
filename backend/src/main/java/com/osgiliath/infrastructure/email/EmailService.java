package com.osgiliath.infrastructure.email;

import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.LineItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Email Service using AWS SES
 * Handles sending invoice emails with PDF attachments
 */
@Service
@Slf4j
public class EmailService {

    private final SesClient sesClient;
    private final String fromEmail;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public EmailService(
            @Value("${aws.ses.region}") String region,
            @Value("${aws.ses.from-email}") String fromEmail) {
        this.fromEmail = fromEmail;
        this.sesClient = SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        log.info("EmailService initialized with region: {} and from email: {}", region, fromEmail);
    }

    /**
     * Send invoice email asynchronously
     * This method runs in a separate thread to not block the transaction
     */
    @Async
    public void sendInvoiceEmail(Invoice invoice, Customer customer, byte[] pdfAttachment) {
        try {
            log.info("Sending invoice email for invoice: {} to customer: {}",
                    invoice.getInvoiceNumber(), customer.getEmailAddress());

            String subject = "Invoice " + invoice.getInvoiceNumber() + " from Osgiliath ERP";
            String htmlBody = buildEmailHtmlBody(invoice, customer);
            String textBody = buildEmailTextBody(invoice, customer);

            sendEmailWithAttachment(
                    customer.getEmailAddress(),
                    subject,
                    htmlBody,
                    textBody,
                    pdfAttachment,
                    "invoice_" + invoice.getInvoiceNumber() + ".pdf"
            );

            log.info("Successfully sent invoice email for invoice: {}", invoice.getInvoiceNumber());
        } catch (Exception e) {
            // Log error but don't throw exception to prevent transaction rollback
            log.error("Failed to send invoice email for invoice: {}. Error: {}",
                    invoice.getInvoiceNumber(), e.getMessage(), e);
        }
    }

    /**
     * Send email with PDF attachment using AWS SES Raw Email API
     */
    private void sendEmailWithAttachment(
            String toEmail,
            String subject,
            String htmlBody,
            String textBody,
            byte[] attachment,
            String attachmentFileName) {

        try {
            // Build raw email message with MIME structure
            String rawMessage = buildRawEmailMessage(toEmail, subject, htmlBody, textBody, attachment, attachmentFileName);

            RawMessage rawEmailMessage = RawMessage.builder()
                    .data(SdkBytes.fromUtf8String(rawMessage))
                    .build();

            SendRawEmailRequest rawEmailRequest = SendRawEmailRequest.builder()
                    .rawMessage(rawEmailMessage)
                    .build();

            sesClient.sendRawEmail(rawEmailRequest);

        } catch (SesException e) {
            log.error("SES Error sending email: {} - {}", e.awsErrorDetails().errorCode(),
                    e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to send email via SES", e);
        } catch (Exception e) {
            log.error("Error sending email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Build MIME multipart email message with HTML body and PDF attachment
     */
    private String buildRawEmailMessage(
            String toEmail,
            String subject,
            String htmlBody,
            String textBody,
            byte[] attachment,
            String attachmentFileName) {

        String boundary = "----=_Part_" + System.currentTimeMillis();
        StringBuilder message = new StringBuilder();

        // Email headers
        message.append("From: ").append(fromEmail).append("\n");
        message.append("To: ").append(toEmail).append("\n");
        message.append("Subject: ").append(subject).append("\n");
        message.append("MIME-Version: 1.0\n");
        message.append("Content-Type: multipart/mixed; boundary=\"").append(boundary).append("\"\n\n");

        // Alternative part for HTML and plain text
        String altBoundary = "----=_Part_Alt_" + System.currentTimeMillis();
        message.append("--").append(boundary).append("\n");
        message.append("Content-Type: multipart/alternative; boundary=\"").append(altBoundary).append("\"\n\n");

        // Plain text part
        message.append("--").append(altBoundary).append("\n");
        message.append("Content-Type: text/plain; charset=UTF-8\n");
        message.append("Content-Transfer-Encoding: 7bit\n\n");
        message.append(textBody).append("\n\n");

        // HTML part
        message.append("--").append(altBoundary).append("\n");
        message.append("Content-Type: text/html; charset=UTF-8\n");
        message.append("Content-Transfer-Encoding: 7bit\n\n");
        message.append(htmlBody).append("\n\n");
        message.append("--").append(altBoundary).append("--\n\n");

        // PDF attachment
        message.append("--").append(boundary).append("\n");
        message.append("Content-Type: application/pdf; name=\"").append(attachmentFileName).append("\"\n");
        message.append("Content-Disposition: attachment; filename=\"").append(attachmentFileName).append("\"\n");
        message.append("Content-Transfer-Encoding: base64\n\n");
        message.append(java.util.Base64.getMimeEncoder().encodeToString(attachment));
        message.append("\n\n");

        // Close boundary
        message.append("--").append(boundary).append("--");

        return message.toString();
    }

    /**
     * Build professional HTML email body with invoice details
     */
    private String buildEmailHtmlBody(Invoice invoice, Customer customer) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append("h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append("th { background-color: #3498db; color: white; padding: 12px; text-align: left; }");
        html.append("td { padding: 10px; border-bottom: 1px solid #ddd; }");
        html.append(".info-box { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0; }");
        html.append(".total-box { background-color: #e8f4f8; padding: 15px; border-radius: 5px; margin: 20px 0; font-weight: bold; }");
        html.append(".footer { margin-top: 30px; padding-top: 20px; border-top: 2px solid #ddd; font-size: 12px; color: #777; text-align: center; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // Header
        html.append("<h1>Invoice ").append(invoice.getInvoiceNumber()).append("</h1>");

        // Customer greeting
        html.append("<p>Dear ").append(customer.getName()).append(",</p>");
        html.append("<p>Thank you for your business! Please find attached your invoice details below.</p>");

        // Invoice information
        html.append("<div class=\"info-box\">");
        html.append("<p><strong>Invoice Number:</strong> ").append(invoice.getInvoiceNumber()).append("</p>");
        html.append("<p><strong>Issue Date:</strong> ").append(invoice.getIssueDate().format(DATE_FORMATTER)).append("</p>");
        html.append("<p><strong>Due Date:</strong> ").append(invoice.getDueDate().format(DATE_FORMATTER)).append("</p>");
        html.append("<p><strong>Status:</strong> ").append(invoice.getStatus()).append("</p>");
        html.append("</div>");

        // Line items table
        html.append("<h2>Invoice Items</h2>");
        html.append("<table>");
        html.append("<thead>");
        html.append("<tr>");
        html.append("<th>Description</th>");
        html.append("<th>Quantity</th>");
        html.append("<th>Unit Price</th>");
        html.append("<th>Amount</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");

        for (LineItem item : invoice.getLineItems()) {
            html.append("<tr>");
            html.append("<td>").append(item.getDescription()).append("</td>");
            html.append("<td>").append(item.getQuantity().stripTrailingZeros().toPlainString()).append("</td>");
            html.append("<td>$").append(item.getUnitPrice().getAmount().toPlainString()).append("</td>");
            html.append("<td>$").append(item.getLineTotal().getAmount().toPlainString()).append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody>");
        html.append("</table>");

        // Totals
        html.append("<div class=\"total-box\">");
        html.append("<p><strong>Subtotal:</strong> $").append(invoice.getSubtotal().getAmount().toPlainString()).append("</p>");
        html.append("<p><strong>Tax:</strong> $").append(invoice.getTaxAmount().getAmount().toPlainString()).append("</p>");
        html.append("<p style=\"font-size: 18px; color: #2c3e50;\"><strong>Total Amount:</strong> $").append(invoice.getTotalAmount().getAmount().toPlainString()).append("</p>");
        html.append("<p style=\"font-size: 18px; color: #e74c3c;\"><strong>Balance Due:</strong> $").append(invoice.getBalanceDue().getAmount().toPlainString()).append("</p>");
        html.append("</div>");

        // Call to action
        html.append("<p>Please find the complete invoice attached as a PDF document.</p>");
        html.append("<p>If you have any questions regarding this invoice, please don't hesitate to contact us.</p>");

        // Footer
        html.append("<div class=\"footer\">");
        html.append("<p>Thank you for choosing Osgiliath ERP!</p>");
        html.append("<p>This is an automated message, please do not reply to this email.</p>");
        html.append("</div>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Build plain text email body as fallback
     */
    private String buildEmailTextBody(Invoice invoice, Customer customer) {
        StringBuilder text = new StringBuilder();

        text.append("Invoice ").append(invoice.getInvoiceNumber()).append("\n");
        text.append("=".repeat(50)).append("\n\n");

        text.append("Dear ").append(customer.getName()).append(",\n\n");
        text.append("Thank you for your business! Please find your invoice details below.\n\n");

        text.append("Invoice Information:\n");
        text.append("- Invoice Number: ").append(invoice.getInvoiceNumber()).append("\n");
        text.append("- Issue Date: ").append(invoice.getIssueDate().format(DATE_FORMATTER)).append("\n");
        text.append("- Due Date: ").append(invoice.getDueDate().format(DATE_FORMATTER)).append("\n");
        text.append("- Status: ").append(invoice.getStatus()).append("\n\n");

        text.append("Invoice Items:\n");
        text.append("-".repeat(50)).append("\n");

        for (LineItem item : invoice.getLineItems()) {
            text.append(item.getDescription()).append("\n");
            text.append("  Quantity: ").append(item.getQuantity().stripTrailingZeros().toPlainString());
            text.append(" x $").append(item.getUnitPrice().getAmount().toPlainString());
            text.append(" = $").append(item.getLineTotal().getAmount().toPlainString()).append("\n");
        }

        text.append("-".repeat(50)).append("\n\n");

        text.append("Subtotal: $").append(invoice.getSubtotal().getAmount().toPlainString()).append("\n");
        text.append("Tax: $").append(invoice.getTaxAmount().getAmount().toPlainString()).append("\n");
        text.append("Total Amount: $").append(invoice.getTotalAmount().getAmount().toPlainString()).append("\n");
        text.append("Balance Due: $").append(invoice.getBalanceDue().getAmount().toPlainString()).append("\n\n");

        text.append("Please find the complete invoice attached as a PDF document.\n\n");
        text.append("If you have any questions regarding this invoice, please don't hesitate to contact us.\n\n");
        text.append("Thank you for choosing Osgiliath ERP!\n\n");
        text.append("This is an automated message, please do not reply to this email.\n");

        return text.toString();
    }
}
