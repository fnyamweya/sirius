package com.sirius.data.adapter;

import com.sirius.core.money.CurrencyCode;
import com.sirius.core.port.BalanceStore;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;
import com.sirius.data.entity.treasury.AccountBalanceEntity;
import com.sirius.data.entity.treasury.AccountEntity;
import com.sirius.data.repository.treasury.AccountBalanceJpaRepository;
import com.sirius.data.repository.treasury.AccountJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaBalanceStoreAdapter implements BalanceStore {

    private final AccountBalanceJpaRepository accountBalanceJpaRepository;
    private final AccountJpaRepository accountJpaRepository;

    public JpaBalanceStoreAdapter(AccountBalanceJpaRepository accountBalanceJpaRepository, AccountJpaRepository accountJpaRepository) {
        this.accountBalanceJpaRepository = accountBalanceJpaRepository;
        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    public Optional<BalanceView> findByAccountId(MarketId marketId, OrgId orgId, UUID accountId) {
                return accountBalanceJpaRepository.findByMarketIdAndOrgIdAndAccountId(marketId.toString(), orgId.value(), accountId)
                                .map(b -> new BalanceView(b.getAccountId(), CurrencyCode.of(b.getCurrency()), b.getAvailableMinor(), b.getLedgerMinor()));
    }

    @Override
        public void reserve(MarketId marketId, OrgId orgId, UUID accountId, long amountMinor, CurrencyCode currency) {
                AccountEntity account = accountJpaRepository.findByMarketIdAndOrgIdAndId(marketId.toString(), orgId.value(), accountId)
                                .orElseThrow();

                AccountBalanceEntity bal = accountBalanceJpaRepository.findByMarketIdAndOrgIdAndAccountId(marketId.toString(), orgId.value(), accountId)
                                .orElseGet(() -> AccountBalanceEntity.builder()
                                                .accountId(accountId)
                                                .marketId(marketId.toString())
                                                .orgId(orgId.value())
                                                .legalEntityId(account.getLegalEntityId())
                                                .currency(currency.toString())
                                                .availableMinor(0)
                                                .reservedMinor(0)
                                                .pendingMinor(0)
                                                .ledgerMinor(0)
                                                .build());

                if (!bal.getCurrency().equals(currency.toString())) {
                        throw new IllegalStateException("Currency mismatch for account balance");
                }

                long availableAfter = bal.getAvailableMinor() - amountMinor;
                if (!account.isAllowOverdraft() && availableAfter < 0) {
                        throw new IllegalStateException("Insufficient available funds");
                }

                bal.setAvailableMinor(availableAfter);
                bal.setReservedMinor(bal.getReservedMinor() + amountMinor);
                accountBalanceJpaRepository.save(bal);
        }

        @Override
        public void releaseReservation(MarketId marketId, OrgId orgId, UUID accountId, long amountMinor, CurrencyCode currency) {
                AccountBalanceEntity bal = accountBalanceJpaRepository.findByMarketIdAndOrgIdAndAccountId(marketId.toString(), orgId.value(), accountId)
                                .orElseThrow();

                if (!bal.getCurrency().equals(currency.toString())) {
                        throw new IllegalStateException("Currency mismatch for account balance");
                }

                long reservedAfter = bal.getReservedMinor() - amountMinor;
                if (reservedAfter < 0) {
                        throw new IllegalStateException("Reservation underflow");
                }

                bal.setReservedMinor(reservedAfter);
                bal.setAvailableMinor(bal.getAvailableMinor() + amountMinor);
                accountBalanceJpaRepository.save(bal);
        }

        @Override
        public void settle(MarketId marketId, OrgId orgId, UUID sourceAccountId, UUID destinationAccountId, long amountMinor, CurrencyCode currency) {
        AccountEntity source = accountJpaRepository.findByMarketIdAndOrgIdAndId(marketId.toString(), orgId.value(), sourceAccountId)
                .orElseThrow();
        AccountEntity dest = accountJpaRepository.findByMarketIdAndOrgIdAndId(marketId.toString(), orgId.value(), destinationAccountId)
                .orElseThrow();

        AccountBalanceEntity sourceBal = accountBalanceJpaRepository.findByMarketIdAndOrgIdAndAccountId(marketId.toString(), orgId.value(), sourceAccountId)
                .orElseGet(() -> AccountBalanceEntity.builder()
                        .accountId(sourceAccountId)
                        .marketId(marketId.toString())
                        .orgId(orgId.value())
                        .legalEntityId(source.getLegalEntityId())
                        .currency(currency.toString())
                        .availableMinor(0)
                        .reservedMinor(0)
                        .pendingMinor(0)
                        .ledgerMinor(0)
                        .build());

        AccountBalanceEntity destBal = accountBalanceJpaRepository.findByMarketIdAndOrgIdAndAccountId(marketId.toString(), orgId.value(), destinationAccountId)
                .orElseGet(() -> AccountBalanceEntity.builder()
                        .accountId(destinationAccountId)
                        .marketId(marketId.toString())
                        .orgId(orgId.value())
                        .legalEntityId(dest.getLegalEntityId())
                        .currency(currency.toString())
                        .availableMinor(0)
                                                .reservedMinor(0)
                                                .pendingMinor(0)
                        .ledgerMinor(0)
                        .build());

                if (!sourceBal.getCurrency().equals(currency.toString()) || !destBal.getCurrency().equals(currency.toString())) {
                        throw new IllegalStateException("Currency mismatch for account balance");
                }

                long reservedAfter = sourceBal.getReservedMinor() - amountMinor;
                if (reservedAfter < 0) {
                        throw new IllegalStateException("Reservation underflow");
                }
                sourceBal.setReservedMinor(reservedAfter);
                sourceBal.setLedgerMinor(sourceBal.getLedgerMinor() - amountMinor);

                destBal.setLedgerMinor(destBal.getLedgerMinor() + amountMinor);
                destBal.setAvailableMinor(destBal.getAvailableMinor() + amountMinor);

        accountBalanceJpaRepository.save(sourceBal);
        accountBalanceJpaRepository.save(destBal);
    }
}
