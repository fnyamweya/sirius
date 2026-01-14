package com.sirius.core.port;

import com.sirius.core.ledger.LedgerEntry;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerStore {

    Optional<byte[]> findLatestHashForAccount(MarketId marketId, OrgId orgId, UUID accountId);

    void appendEntries(List<LedgerEntry> entries);
}
