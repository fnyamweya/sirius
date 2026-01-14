package com.sirius.core.port;

import com.sirius.core.money.CurrencyCode;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;

import java.util.Optional;
import java.util.UUID;

public interface BalanceStore {

    record BalanceView(UUID accountId, CurrencyCode currency, long availableMinor, long ledgerMinor) {
    }

    Optional<BalanceView> findByAccountId(MarketId marketId, OrgId orgId, UUID accountId);

    /**
     * Reserve funds for a transfer initiation.
     * Implementations must enforce no-negative-available unless allowOverdraft=true on the account.
     */
    void reserve(MarketId marketId, OrgId orgId, UUID accountId, long amountMinor, CurrencyCode currency);

    /**
     * Release a prior reservation (e.g. cancel/fail).
     */
    void releaseReservation(MarketId marketId, OrgId orgId, UUID accountId, long amountMinor, CurrencyCode currency);

    /**
     * Settle a transfer: move reserved funds on source, and credit destination.
     */
    void settle(MarketId marketId, OrgId orgId, UUID sourceAccountId, UUID destinationAccountId, long amountMinor, CurrencyCode currency);
}
