package com.sirius.core.port;

import com.sirius.core.account.AccountStatus;
import com.sirius.core.money.CurrencyCode;
import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;

import java.util.Optional;
import java.util.UUID;

public interface AccountStore {

    record AccountView(UUID id, MarketId marketId, OrgId orgId, LegalEntityId legalEntityId, CurrencyCode currency, AccountStatus status) {
    }

    Optional<AccountView> findById(MarketId marketId, OrgId orgId, UUID accountId);
}
