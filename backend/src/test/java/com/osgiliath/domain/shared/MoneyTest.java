package com.osgiliath.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Money value object
 * Tests all money operations and validation logic
 */
@DisplayName("Money Value Object")
class MoneyTest {

    @Test
    @DisplayName("Should create money from BigDecimal")
    void shouldCreateMoneyFromBigDecimal() {
        Money money = Money.of(new BigDecimal("100.50"));

        assertThat(money.getAmount()).isEqualByComparingTo("100.50");
    }

    @Test
    @DisplayName("Should create money from double")
    void shouldCreateMoneyFromDouble() {
        Money money = Money.of(100.50);

        assertThat(money.getAmount()).isEqualByComparingTo("100.50");
    }

    @Test
    @DisplayName("Should create zero money")
    void shouldCreateZeroMoney() {
        Money money = Money.zero();

        assertThat(money.getAmount()).isEqualByComparingTo("0.00");
        assertThat(money.isZero()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when amount is null")
    void shouldThrowExceptionWhenAmountIsNull() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount cannot be null");
    }

    @Test
    @DisplayName("Should round to 2 decimal places")
    void shouldRoundToTwoDecimalPlaces() {
        Money money = Money.of(new BigDecimal("100.556"));

        assertThat(money.getAmount()).isEqualByComparingTo("100.56");
    }

    @Test
    @DisplayName("Should add two money amounts")
    void shouldAddTwoMoneyAmounts() {
        Money money1 = Money.of(100.00);
        Money money2 = Money.of(50.50);

        Money result = money1.add(money2);

        assertThat(result.getAmount()).isEqualByComparingTo("150.50");
    }

    @Test
    @DisplayName("Should subtract two money amounts")
    void shouldSubtractTwoMoneyAmounts() {
        Money money1 = Money.of(100.00);
        Money money2 = Money.of(30.50);

        Money result = money1.subtract(money2);

        assertThat(result.getAmount()).isEqualByComparingTo("69.50");
    }

    @Test
    @DisplayName("Should multiply money by BigDecimal")
    void shouldMultiplyMoneyByBigDecimal() {
        Money money = Money.of(100.00);
        BigDecimal multiplier = new BigDecimal("1.5");

        Money result = money.multiply(multiplier);

        assertThat(result.getAmount()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Should round after multiplication")
    void shouldRoundAfterMultiplication() {
        Money money = Money.of(100.00);
        BigDecimal multiplier = new BigDecimal("0.333");

        Money result = money.multiply(multiplier);

        assertThat(result.getAmount()).isEqualByComparingTo("33.30");
    }

    @Test
    @DisplayName("Should compare greater than correctly")
    void shouldCompareGreaterThan() {
        Money larger = Money.of(100.00);
        Money smaller = Money.of(50.00);

        assertThat(larger.isGreaterThan(smaller)).isTrue();
        assertThat(smaller.isGreaterThan(larger)).isFalse();
    }

    @Test
    @DisplayName("Should compare greater than or equal correctly")
    void shouldCompareGreaterThanOrEqual() {
        Money money1 = Money.of(100.00);
        Money money2 = Money.of(100.00);
        Money smaller = Money.of(50.00);

        assertThat(money1.isGreaterThanOrEqual(money2)).isTrue();
        assertThat(money1.isGreaterThanOrEqual(smaller)).isTrue();
        assertThat(smaller.isGreaterThanOrEqual(money1)).isFalse();
    }

    @Test
    @DisplayName("Should compare less than correctly")
    void shouldCompareLessThan() {
        Money smaller = Money.of(50.00);
        Money larger = Money.of(100.00);

        assertThat(smaller.isLessThan(larger)).isTrue();
        assertThat(larger.isLessThan(smaller)).isFalse();
    }

    @Test
    @DisplayName("Should identify zero amounts")
    void shouldIdentifyZeroAmounts() {
        Money zero = Money.zero();
        Money nonZero = Money.of(0.01);

        assertThat(zero.isZero()).isTrue();
        assertThat(nonZero.isZero()).isFalse();
    }

    @Test
    @DisplayName("Should identify negative amounts")
    void shouldIdentifyNegativeAmounts() {
        Money negative = Money.of(-10.00);
        Money positive = Money.of(10.00);
        Money zero = Money.zero();

        assertThat(negative.isNegative()).isTrue();
        assertThat(positive.isNegative()).isFalse();
        assertThat(zero.isNegative()).isFalse();
    }

    @Test
    @DisplayName("Should be immutable")
    void shouldBeImmutable() {
        Money original = Money.of(100.00);
        Money added = original.add(Money.of(50.00));

        assertThat(original.getAmount()).isEqualByComparingTo("100.00");
        assertThat(added.getAmount()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Should have correct equals behavior")
    void shouldHaveCorrectEqualsBehavior() {
        Money money1 = Money.of(100.00);
        Money money2 = Money.of(100.00);
        Money money3 = Money.of(200.00);

        assertThat(money1).isEqualTo(money2);
        assertThat(money1).isNotEqualTo(money3);
        assertThat(money1).isNotEqualTo(null);
        assertThat(money1).isNotEqualTo("100.00");
    }

    @Test
    @DisplayName("Should have consistent hashCode")
    void shouldHaveConsistentHashCode() {
        Money money1 = Money.of(100.00);
        Money money2 = Money.of(100.00);

        assertThat(money1.hashCode()).isEqualTo(money2.hashCode());
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
        Money money = Money.of(100.50);

        assertThat(money.toString()).isEqualTo("100.50");
    }
}
