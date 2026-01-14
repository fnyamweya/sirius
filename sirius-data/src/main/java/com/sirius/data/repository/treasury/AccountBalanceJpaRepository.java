package com.sirius.data.repository.treasury;

import com.sirius.data.entity.treasury.AccountBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountBalanceJpaRepository extends JpaRepository<AccountBalanceEntity, UUID> {

    Optional<AccountBalanceEntity> findByMarketIdAndOrgIdAndAccountId(String marketId, UUID orgId, UUID accountId);
}
