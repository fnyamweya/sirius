package com.sirius.core.port;

import com.sirius.core.ledger.JournalEntry;
import com.sirius.core.ledger.JournalLine;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;

import java.util.List;
import java.util.Optional;

public interface JournalStore {

    Optional<byte[]> findLatestHash(MarketId marketId, OrgId orgId);

    void appendPosted(JournalEntry entry, List<JournalLine> lines);
}
