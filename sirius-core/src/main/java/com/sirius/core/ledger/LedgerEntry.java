package com.sirius.core.ledger;

import com.sirius.core.money.CurrencyCode;
import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntry(
        UUID id,
        MarketId marketId,
        OrgId orgId,
        LegalEntityId legalEntityId,
        UUID accountId,
        UUID transferId,
        LedgerDirection direction,
        CurrencyCode currency,
        long amountMinor,
        Instant occurredAt,
        byte[] prevHash,
        byte[] entryHash
) {
}
