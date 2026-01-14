package com.sirius.api.dto;

import com.sirius.core.transfer.TransferStatus;

import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        String market_id,
        String org_id,
        String legal_entity_id,
        UUID source_account_id,
        UUID destination_account_id,
        MoneyDto amount,
        TransferStatus status,
        String reason,
        Instant created_at,
        Instant updated_at
) {
}
