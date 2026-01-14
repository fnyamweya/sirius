package com.sirius.core.money;

import java.util.Objects;

public final class Money {

    private final long amountMinor;
    private final CurrencyCode currency;

    private Money(long amountMinor, CurrencyCode currency) {
        this.amountMinor = amountMinor;
        this.currency = currency;
    }

    public static Money ofMinor(long amountMinor, CurrencyCode currency) {
        if (currency == null) {
            throw new IllegalArgumentException("currency is required");
        }
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return new Money(amountMinor, currency);
    }

    public static Money ofMinor(long amountMinor, String currency) {
        return ofMinor(amountMinor, CurrencyCode.of(currency));
    }

    public long amountMinor() {
        return amountMinor;
    }

    public CurrencyCode currency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amountMinor == money.amountMinor && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountMinor, currency);
    }

    @Override
    public String toString() {
        return "Money[amountMinor=" + amountMinor + ", currency=" + currency + "]";
    }
}
