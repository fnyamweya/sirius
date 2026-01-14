package com.sirius.data.repository.treasury;

import com.sirius.data.entity.treasury.ReconciliationRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReconciliationRunJpaRepository extends JpaRepository<ReconciliationRunEntity, UUID> {

    Optional<ReconciliationRunEntity> findByMarketIdAndOrgIdAndId(String marketId, UUID orgId, UUID id);

    Optional<ReconciliationRunEntity> findByMarketIdAndOrgIdAndLegalEntityIdAndId(String marketId, UUID orgId, UUID legalEntityId, UUID id);
}
