package com.sirius.data.adapter;

import com.sirius.core.port.OutboxStore;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;
import com.sirius.data.entity.treasury.OutboxEntity;
import com.sirius.data.repository.treasury.OutboxJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaOutboxStoreAdapter implements OutboxStore {

    private final OutboxJpaRepository outboxJpaRepository;

    public JpaOutboxStoreAdapter(OutboxJpaRepository outboxJpaRepository) {
        this.outboxJpaRepository = outboxJpaRepository;
    }

    @Override
    public void add(OutboxRecord record) {
        outboxJpaRepository.save(OutboxEntity.builder()
                .id(record.id())
                .marketId(record.marketId().toString())
                .orgId(record.orgId().value())
                .aggregateType(record.aggregateType())
                .aggregateId(record.aggregateId())
                .eventType(record.eventType())
                .payload(record.payloadJson())
                .dedupeKey(record.dedupeKey())
                .createdAt(record.createdAt())
                .publishedAt(record.publishedAt())
                .build());
    }

    @Override
    public Optional<OutboxRecord> nextUnpublished(MarketId marketId, OrgId orgId) {
        return outboxJpaRepository.findNextUnpublished(marketId.toString(), orgId.value())
                .map(o -> new OutboxRecord(
                        o.getId(),
                        MarketId.of(o.getMarketId()),
                        OrgId.of(o.getOrgId()),
                        o.getAggregateType(),
                        o.getAggregateId(),
                        o.getEventType(),
                        o.getPayload(),
                        o.getDedupeKey(),
                        o.getCreatedAt(),
                        o.getPublishedAt()
                ));
    }

    @Override
    public void markPublished(UUID outboxId, Instant publishedAt) {
        outboxJpaRepository.findById(outboxId).ifPresent(o -> {
            o.setPublishedAt(publishedAt);
            outboxJpaRepository.save(o);
        });
    }
}
