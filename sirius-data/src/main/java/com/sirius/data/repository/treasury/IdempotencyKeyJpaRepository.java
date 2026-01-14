package com.sirius.data.repository.treasury;

import com.sirius.data.entity.treasury.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, Long> {

    Optional<IdempotencyKeyEntity> findByMarketIdAndOrgIdAndIdempotencyKey(String marketId, UUID orgId, String idempotencyKey);
}
