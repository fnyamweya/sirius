package com.sirius.data.repository.treasury;

import com.sirius.data.entity.treasury.TransferEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransferJpaRepository extends JpaRepository<TransferEntity, UUID> {

    Optional<TransferEntity> findByMarketIdAndOrgIdAndId(String marketId, UUID orgId, UUID id);
}
