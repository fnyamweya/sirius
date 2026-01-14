package com.sirius.core.money;

import java.util.Currency;

public record CurrencyCode(String value) {

    public static CurrencyCode of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        String normalized = value.trim().toUpperCase();
        Currency.getInstance(normalized);
        return new CurrencyCode(normalized);
    }

    @Override
    public String toString() {
        return value;
    }
}
