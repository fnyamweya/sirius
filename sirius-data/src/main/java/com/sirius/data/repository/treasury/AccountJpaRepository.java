package com.sirius.data.repository.treasury;

import com.sirius.data.entity.treasury.AccountEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByMarketIdAndOrgIdAndId(String marketId, UUID orgId, UUID id);

    Page<AccountEntity> findByMarketIdAndOrgIdAndNameContainingIgnoreCase(String marketId, UUID orgId, String name, Pageable pageable);

    Page<AccountEntity> findByMarketIdAndOrgIdAndExternalRefContainingIgnoreCase(String marketId, UUID orgId, String externalRef, Pageable pageable);
}
