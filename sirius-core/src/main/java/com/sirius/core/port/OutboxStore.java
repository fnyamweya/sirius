package com.sirius.core.port;

import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OutboxStore {

    record OutboxRecord(UUID id, MarketId marketId, OrgId orgId, String aggregateType, UUID aggregateId, String eventType, String payloadJson, String dedupeKey, Instant createdAt, Instant publishedAt) {
    }

    void add(OutboxRecord record);

    Optional<OutboxRecord> nextUnpublished(MarketId marketId, OrgId orgId);

    void markPublished(UUID outboxId, Instant publishedAt);
}
