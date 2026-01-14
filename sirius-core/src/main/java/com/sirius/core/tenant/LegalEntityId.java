package com.sirius.core.tenant;

import java.util.UUID;

public record LegalEntityId(UUID value) {

    public static LegalEntityId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("legal_entity_id is required");
        }
        try {
            return new LegalEntityId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("legal_entity_id must be a UUID");
        }
    }

    public static LegalEntityId of(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("legal_entity_id is required");
        }
        return new LegalEntityId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
