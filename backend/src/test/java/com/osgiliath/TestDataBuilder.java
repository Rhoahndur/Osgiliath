package com.osgiliath;

import com.osgiliath.domain.customer.Customer;
import com.osgiliath.domain.customer.CustomerRepository;
import com.osgiliath.domain.invoice.Invoice;
import com.osgiliath.domain.invoice.InvoiceRepository;
import com.osgiliath.domain.payment.Payment;
import com.osgiliath.domain.payment.PaymentMethod;
import com.osgiliath.domain.payment.PaymentRepository;
import com.osgiliath.domain.shared.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Builder for creating test data with sensible defaults
 * Simplifies test setup and makes tests more readable
 */
@Component
@RequiredArgsConstructor
public class TestDataBuilder {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    // Customer Builder
    public CustomerBuilder customer() {
        return new CustomerBuilder();
    }

    public class CustomerBuilder {
        private String name = "Test Customer";
        private String email = "test@example.com";
        private String phone = "555-1234";
        private String address = "123 Test Street";

        public CustomerBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CustomerBuilder email(String email) {
            this.email = email;
            return this;
        }

        public CustomerBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public CustomerBuilder address(String address) {
            this.address = address;
            return this;
        }

        public Customer build() {
            return Customer.create(name, email, phone, address);
        }

        public Customer buildAndSave() {
            return customerRepository.save(build());
        }
    }

    // Invoice Builder
    public InvoiceBuilder invoice() {
        return new InvoiceBuilder();
    }

    public class InvoiceBuilder {
        private Customer customer;
        private String invoiceNumber = "INV-" + System.currentTimeMillis();
        private LocalDate issueDate = LocalDate.now();
        private LocalDate dueDate = LocalDate.now().plusDays(30);

        public InvoiceBuilder customer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public InvoiceBuilder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public InvoiceBuilder issueDate(LocalDate issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        public InvoiceBuilder dueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Invoice build() {
            if (customer == null) {
                customer = TestDataBuilder.this.customer().buildAndSave();
            }
            return Invoice.create(customer.getId(), invoiceNumber, issueDate, dueDate);
        }

        public Invoice buildAndSave() {
            return invoiceRepository.save(build());
        }

        public Invoice buildWithLineItems() {
            Invoice invoice = build();
            invoice.addLineItem("Service A", BigDecimal.valueOf(2), Money.of(100.0));
            invoice.addLineItem("Service B", BigDecimal.valueOf(1), Money.of(50.0));
            return invoice;
        }

        public Invoice buildWithLineItemsAndSave() {
            return invoiceRepository.save(buildWithLineItems());
        }

        public Invoice buildSent() {
            Invoice invoice = buildWithLineItems();
            invoice.send();
            return invoice;
        }

        public Invoice buildSentAndSave() {
            return invoiceRepository.save(buildSent());
        }
    }

    // Payment Builder
    public PaymentBuilder payment() {
        return new PaymentBuilder();
    }

    public class PaymentBuilder {
        private Invoice invoice;
        private LocalDate paymentDate = LocalDate.now();
        private Money amount = Money.of(100.0);
        private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;
        private String referenceNumber = "REF-" + System.currentTimeMillis();

        public PaymentBuilder invoice(Invoice invoice) {
            this.invoice = invoice;
            return this;
        }

        public PaymentBuilder paymentDate(LocalDate paymentDate) {
            this.paymentDate = paymentDate;
            return this;
        }

        public PaymentBuilder amount(Money amount) {
            this.amount = amount;
            return this;
        }

        public PaymentBuilder amount(double amount) {
            this.amount = Money.of(amount);
            return this;
        }

        public PaymentBuilder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public PaymentBuilder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }

        public Payment build() {
            if (invoice == null) {
                invoice = TestDataBuilder.this.invoice().buildSentAndSave();
            }
            return Payment.create(
                    invoice.getId(),
                    paymentDate,
                    amount,
                    paymentMethod,
                    referenceNumber
            );
        }

        public Payment buildAndSave() {
            return paymentRepository.save(build());
        }
    }
}
