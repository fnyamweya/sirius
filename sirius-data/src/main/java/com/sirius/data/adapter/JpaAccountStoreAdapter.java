package com.sirius.data.adapter;

import com.sirius.core.account.AccountStatus;
import com.sirius.core.money.CurrencyCode;
import com.sirius.core.port.AccountStore;
import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;
import com.sirius.data.repository.treasury.AccountJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaAccountStoreAdapter implements AccountStore {

    private final AccountJpaRepository accountJpaRepository;

    public JpaAccountStoreAdapter(AccountJpaRepository accountJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    public Optional<AccountView> findById(MarketId marketId, OrgId orgId, UUID accountId) {
        return accountJpaRepository.findByMarketIdAndOrgIdAndId(marketId.toString(), orgId.value(), accountId)
                .map(e -> new AccountView(
                        e.getId(),
                        MarketId.of(e.getMarketId()),
                        OrgId.of(e.getOrgId()),
                        LegalEntityId.of(e.getLegalEntityId()),
                        CurrencyCode.of(e.getCurrency()),
                        e.getStatus() == null ? AccountStatus.ACTIVE : e.getStatus()
                ));
    }
}
