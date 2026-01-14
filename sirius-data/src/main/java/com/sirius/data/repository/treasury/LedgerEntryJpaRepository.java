package com.sirius.data.repository.treasury;

import com.sirius.data.entity.treasury.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    Optional<LedgerEntryEntity> findTopByMarketIdAndOrgIdAndAccountIdOrderByOccurredAtDesc(String marketId, UUID orgId, UUID accountId);
}
