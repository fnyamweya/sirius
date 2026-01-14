package com.sirius.api.dto;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID account_id,
        UUID transfer_id,
        String direction,
        String currency,
        long amount_minor,
        Instant occurred_at,
        String entry_hash
) {
}
