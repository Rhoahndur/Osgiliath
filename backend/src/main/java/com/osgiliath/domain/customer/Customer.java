package com.osgiliath.domain.customer;

import com.osgiliath.domain.shared.BaseEntity;
import com.osgiliath.domain.shared.DomainException;
import com.osgiliath.domain.shared.Email;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Customer Aggregate Root
 * Encapsulates customer business logic and invariants
 */
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_email", columnList = "email_address", unique = true)
})
@Getter
@NoArgsConstructor
public class Customer extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "email_address", nullable = false, unique = true))
    private Email email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    // Private constructor for aggregate creation
    private Customer(String name, Email email, String phone, String address) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    /**
     * Factory method to create a new customer
     */
    public static Customer create(String name, String emailAddress, String phone, String address) {
        validateName(name);
        Email email = Email.of(emailAddress);

        return new Customer(name, email, phone, address);
    }

    /**
     * Update customer information
     */
    public void update(String name, String emailAddress, String phone, String address) {
        validateName(name);
        Email newEmail = Email.of(emailAddress);

        this.name = name;
        this.email = newEmail;
        this.phone = phone;
        this.address = address;
    }

    /**
     * Business rule: Name must not be empty
     */
    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Customer name cannot be empty");
        }
        if (name.length() > 200) {
            throw new DomainException("Customer name cannot exceed 200 characters");
        }
    }

    public String getEmailAddress() {
        return email != null ? email.getAddress() : null;
    }
}
