package com.sirius.data.adapter;

import com.sirius.core.money.CurrencyCode;
import com.sirius.core.money.Money;
import com.sirius.core.port.TransferStore;
import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;
import com.sirius.core.transfer.Transfer;
import com.sirius.core.transfer.TransferType;
import com.sirius.data.entity.treasury.TransferEntity;
import com.sirius.data.repository.treasury.TransferJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaTransferStoreAdapter implements TransferStore {

    private final TransferJpaRepository transferJpaRepository;

    public JpaTransferStoreAdapter(TransferJpaRepository transferJpaRepository) {
        this.transferJpaRepository = transferJpaRepository;
    }

    @Override
    public Transfer save(Transfer transfer) {
        TransferEntity entity = toEntity(transfer);
        TransferEntity saved = transferJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Transfer> findById(MarketId marketId, OrgId orgId, UUID transferId) {
        return transferJpaRepository.findByMarketIdAndOrgIdAndId(marketId.toString(), orgId.value(), transferId)
                .map(JpaTransferStoreAdapter::toDomain);
    }

    @Override
    public void update(Transfer transfer) {
        transferJpaRepository.save(toEntity(transfer));
    }

    private static TransferEntity toEntity(Transfer t) {
        return TransferEntity.builder()
                .id(t.id())
                .marketId(t.marketId().toString())
                .orgId(t.orgId().value())
                .legalEntityId(t.legalEntityId().value())
                .sourceAccountId(t.sourceAccountId())
                .destinationAccountId(t.destinationAccountId())
                .currency(t.money().currency().toString())
                .amountMinor(t.money().amountMinor())
                .transferType(t.type() == null ? TransferType.INTERNAL : t.type())
                .status(t.status())
                .reason(t.reason())
                .createdBySubject(t.createdBySubject())
                .approvedBySubject(t.approvedBySubject())
                .canceledBySubject(t.canceledBySubject())
                .failedReason(t.failedReason())
                .createdAt(t.createdAt())
                .updatedAt(t.updatedAt())
                .build();
    }

    private static Transfer toDomain(TransferEntity e) {
        return new Transfer(
                e.getId(),
                MarketId.of(e.getMarketId()),
                OrgId.of(e.getOrgId()),
                LegalEntityId.of(e.getLegalEntityId()),
                e.getSourceAccountId(),
                e.getDestinationAccountId(),
            Money.ofMinor(e.getAmountMinor(), CurrencyCode.of(e.getCurrency())),
                e.getTransferType(),
                e.getStatus(),
                e.getReason(),
                e.getCreatedBySubject(),
                e.getApprovedBySubject(),
                e.getCanceledBySubject(),
                e.getFailedReason(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
