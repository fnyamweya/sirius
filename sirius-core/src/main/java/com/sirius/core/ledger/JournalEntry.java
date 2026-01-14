package com.sirius.core.ledger;

import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;

import java.time.Instant;
import java.util.UUID;

public record JournalEntry(
        UUID id,
        MarketId marketId,
        OrgId orgId,
        String correlationId,
        String referenceType,
        UUID referenceId,
        JournalStatus status,
        Instant postedAt,
        byte[] prevHash,
        byte[] entryHash
) {
}
