package com.sirius.core.tenant;

import java.util.Objects;

public final class MarketId {

    private final String value;

    private MarketId(String value) {
        this.value = value;
    }

    public static MarketId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("market_id is required");
        }
        return new MarketId(value.trim());
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketId marketId = (MarketId) o;
        return Objects.equals(value, marketId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
