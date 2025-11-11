package com.osgiliath.domain.invoice;

import com.osgiliath.domain.shared.BaseEntity;
import com.osgiliath.domain.shared.DomainException;
import com.osgiliath.domain.shared.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * LineItem Entity - part of Invoice aggregate
 */
@Entity
@Table(name = "line_items")
@Getter
@NoArgsConstructor
public class LineItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price", nullable = false))
    private Money unitPrice;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "line_total", nullable = false))
    private Money lineTotal;

    // Package-private constructor for aggregate
    LineItem(Invoice invoice, String description, BigDecimal quantity, Money unitPrice) {
        validateDescription(description);
        validateQuantity(quantity);
        validateUnitPrice(unitPrice);

        this.invoice = invoice;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(quantity);
    }

    /**
     * Update line item
     */
    void update(String description, BigDecimal quantity, Money unitPrice) {
        validateDescription(description);
        validateQuantity(quantity);
        validateUnitPrice(unitPrice);

        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(quantity);
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new DomainException("Line item description cannot be empty");
        }
        if (description.length() > 500) {
            throw new DomainException("Line item description cannot exceed 500 characters");
        }
    }

    private void validateQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Line item quantity must be greater than zero");
        }
    }

    private void validateUnitPrice(Money unitPrice) {
        if (unitPrice == null || unitPrice.isNegative()) {
            throw new DomainException("Line item unit price cannot be negative");
        }
    }
}
