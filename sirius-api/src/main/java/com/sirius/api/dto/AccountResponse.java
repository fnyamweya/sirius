package com.sirius.api.dto;

import com.sirius.core.account.AccountStatus;

import java.util.UUID;

public record AccountResponse(
        UUID id,
        String market_id,
        String org_id,
        String legal_entity_id,
        String currency,
        AccountStatus status,
        String name,
        String external_ref
) {
}
