package com.sirius.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ReconciliationRunResponse(
        UUID id,
        String market_id,
        String org_id,
        String legal_entity_id,
        String status,
        Instant started_at,
        Instant completed_at
) {
}
