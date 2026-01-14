package com.sirius.core.tenant;

import java.util.Objects;
import java.util.UUID;

public final class OrgId {

    private final UUID value;

    private OrgId(UUID value) {
        this.value = value;
    }

    public static OrgId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("org_id is required");
        }
        try {
            return new OrgId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("org_id must be a UUID");
        }
    }

    public static OrgId of(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("org_id is required");
        }
        return new OrgId(value);
    }

    public UUID value() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrgId orgId = (OrgId) o;
        return Objects.equals(value, orgId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
