package dev.huntertagog.coresystem.platform.economy;

import java.util.Objects;

/**
 * Immutable Value-Object für Beträge.
 * amount = Basis-Einheit (z. B. 1 = 1 Coin), keine Floating-Points.
 */
public record Money(CurrencyId currency, long amount) {

    public Money(CurrencyId currency, long amount) {
        this.currency = Objects.requireNonNull(currency, "currency");
        this.amount = amount;
    }

    public static Money of(CurrencyId currency, long amount) {
        return new Money(currency, amount);
    }

    public boolean isNegative() {
        return amount < 0;
    }

    public boolean isZeroOrNegative() {
        return amount <= 0;
    }

    public boolean isZero() {
        return amount == 0;
    }

    public Money add(long delta) {
        return new Money(currency, this.amount + delta);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(currency, this.amount + other.amount);
    }

    public Money subtract(long delta) {
        return new Money(currency, this.amount - delta);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(currency, this.amount - other.amount);
    }

    public boolean gte(Money other) {
        assertSameCurrency(other);
        return this.amount >= other.amount;
    }

    public boolean lt(Money other) {
        assertSameCurrency(other);
        return this.amount < other.amount;
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public String toString() {
        return amount + " " + currency.value();
    }
}
