package com.sirius.data.adapter;

import com.sirius.core.ledger.LedgerEntry;
import com.sirius.core.port.LedgerStore;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;
import com.sirius.data.entity.treasury.LedgerEntryEntity;
import com.sirius.data.repository.treasury.LedgerEntryJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaLedgerStoreAdapter implements LedgerStore {

    private final LedgerEntryJpaRepository ledgerEntryJpaRepository;

    public JpaLedgerStoreAdapter(LedgerEntryJpaRepository ledgerEntryJpaRepository) {
        this.ledgerEntryJpaRepository = ledgerEntryJpaRepository;
    }

    @Override
    public Optional<byte[]> findLatestHashForAccount(MarketId marketId, OrgId orgId, UUID accountId) {
        return ledgerEntryJpaRepository
                .findTopByMarketIdAndOrgIdAndAccountIdOrderByOccurredAtDesc(marketId.toString(), orgId.value(), accountId)
                .map(LedgerEntryEntity::getEntryHash);
    }

    @Override
    public void appendEntries(List<LedgerEntry> entries) {
        List<LedgerEntryEntity> entities = entries.stream().map(JpaLedgerStoreAdapter::toEntity).toList();
        ledgerEntryJpaRepository.saveAll(entities);
    }

    private static LedgerEntryEntity toEntity(LedgerEntry e) {
        return LedgerEntryEntity.builder()
                .id(e.id())
                .marketId(e.marketId().toString())
                .orgId(e.orgId().value())
                .legalEntityId(e.legalEntityId().value())
                .accountId(e.accountId())
                .transferId(e.transferId())
                .direction(e.direction())
                .currency(e.currency().toString())
                .amountMinor(e.amountMinor())
                .occurredAt(e.occurredAt())
                .prevHash(e.prevHash())
                .entryHash(e.entryHash())
                .build();
    }
}
