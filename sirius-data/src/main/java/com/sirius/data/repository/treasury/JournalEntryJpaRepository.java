package com.sirius.data.repository.treasury;

import com.sirius.data.entity.treasury.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> {

    Optional<JournalEntryEntity> findTopByMarketIdAndOrgIdOrderByPostedAtDesc(String marketId, UUID orgId);
}
