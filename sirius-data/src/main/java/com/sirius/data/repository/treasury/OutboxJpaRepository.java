package com.sirius.data.repository.treasury;

import com.sirius.data.entity.treasury.OutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OutboxJpaRepository extends JpaRepository<OutboxEntity, UUID> {

    @Query("select o from OutboxEntity o where o.marketId = :marketId and o.orgId = :orgId and o.publishedAt is null order by o.createdAt asc")
    Optional<OutboxEntity> findNextUnpublished(@Param("marketId") String marketId, @Param("orgId") UUID orgId);
}
