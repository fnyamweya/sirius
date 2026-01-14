package com.sirius.data.adapter;

import com.sirius.core.ledger.JournalEntry;
import com.sirius.core.ledger.JournalLine;
import com.sirius.core.money.CurrencyCode;
import com.sirius.core.port.JournalStore;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;
import com.sirius.data.entity.treasury.JournalEntryEntity;
import com.sirius.data.entity.treasury.JournalLineEntity;
import com.sirius.data.repository.treasury.JournalEntryJpaRepository;
import com.sirius.data.repository.treasury.JournalLineJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaJournalStoreAdapter implements JournalStore {

    private final JournalEntryJpaRepository entryRepo;
    private final JournalLineJpaRepository lineRepo;

    public JpaJournalStoreAdapter(JournalEntryJpaRepository entryRepo, JournalLineJpaRepository lineRepo) {
        this.entryRepo = entryRepo;
        this.lineRepo = lineRepo;
    }

    @Override
    public Optional<byte[]> findLatestHash(MarketId marketId, OrgId orgId) {
        return entryRepo.findTopByMarketIdAndOrgIdOrderByPostedAtDesc(marketId.toString(), orgId.value())
                .map(JournalEntryEntity::getEntryHash);
    }

    @Override
    public void appendPosted(JournalEntry entry, List<JournalLine> lines) {
        entryRepo.save(JournalEntryEntity.builder()
                .id(entry.id())
                .marketId(entry.marketId().toString())
            .orgId(entry.orgId().value())
                .correlationId(entry.correlationId())
                .referenceType(entry.referenceType())
                .referenceId(entry.referenceId())
                .status(entry.status())
                .postedAt(entry.postedAt())
                .prevHash(entry.prevHash())
                .entryHash(entry.entryHash())
                .build());

        List<JournalLineEntity> entities = lines.stream().map(l -> JournalLineEntity.builder()
                .id(l.id())
                .entryId(l.entryId())
                .accountId(l.accountId())
                .currency(l.currency().toString())
                .direction(l.direction())
                .lineType(l.lineType())
                .amount(l.amount())
                .memo(l.memo())
                .build()).toList();

        lineRepo.saveAll(entities);
    }
}
