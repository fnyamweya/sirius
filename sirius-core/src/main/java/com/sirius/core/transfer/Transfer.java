package com.sirius.core.transfer;

import com.sirius.core.exception.ConflictException;
import com.sirius.core.money.Money;
import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class Transfer {

    private final UUID id;
    private final MarketId marketId;
    private final OrgId orgId;
    private final LegalEntityId legalEntityId;

    private final UUID sourceAccountId;
    private final UUID destinationAccountId;

    private final Money money;

    private final TransferType type;

    private TransferStatus status;
    private String reason;

    private String failedReason;

    private final String createdBySubject;
    private String approvedBySubject;
    private String canceledBySubject;

    private final Instant createdAt;
    private Instant updatedAt;

    public Transfer(
            UUID id,
            MarketId marketId,
            OrgId orgId,
            LegalEntityId legalEntityId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            Money money,
            TransferType type,
            TransferStatus status,
            String reason,
            String createdBySubject,
            String approvedBySubject,
            String canceledBySubject,
            String failedReason,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (id == null) throw new IllegalArgumentException("id is required");
        if (marketId == null) throw new IllegalArgumentException("marketId is required");
        if (orgId == null) throw new IllegalArgumentException("orgId is required");
        if (legalEntityId == null) throw new IllegalArgumentException("legalEntityId is required");
        if (sourceAccountId == null) throw new IllegalArgumentException("sourceAccountId is required");
        if (destinationAccountId == null) throw new IllegalArgumentException("destinationAccountId is required");
        if (sourceAccountId.equals(destinationAccountId)) throw new IllegalArgumentException("accounts must differ");
        if (money == null) throw new IllegalArgumentException("money is required");
        if (createdBySubject == null || createdBySubject.isBlank()) throw new IllegalArgumentException("createdBySubject is required");

        this.id = id;
        this.marketId = marketId;
        this.orgId = orgId;
        this.legalEntityId = legalEntityId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.money = money;
        this.type = type == null ? TransferType.INTERNAL : type;
        this.status = status == null ? TransferStatus.PENDING_APPROVAL : status;
        this.reason = reason;
        this.failedReason = failedReason;
        this.createdBySubject = createdBySubject;
        this.approvedBySubject = approvedBySubject;
        this.canceledBySubject = canceledBySubject;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
        this.updatedAt = updatedAt == null ? this.createdAt : updatedAt;
    }

    public static Transfer newPending(
            MarketId marketId,
            OrgId orgId,
            LegalEntityId legalEntityId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            Money money,
            String createdBySubject,
            String reason
    ) {
        return new Transfer(
                UUID.randomUUID(),
                marketId,
                orgId,
                legalEntityId,
                sourceAccountId,
                destinationAccountId,
                money,
                TransferType.INTERNAL,
                TransferStatus.PENDING_APPROVAL,
                reason,
                createdBySubject,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    public void queue(String subject) {
        if (status != TransferStatus.PENDING_APPROVAL) {
            throw new ConflictException("Transfer cannot be queued in current state",
                    Map.of("transfer_id", id.toString(), "status", status.name()));
        }
        this.status = TransferStatus.QUEUED;
        this.approvedBySubject = subject;
        this.updatedAt = Instant.now();
    }

    public void startProcessing() {
        if (status != TransferStatus.QUEUED) {
            throw new ConflictException("Transfer cannot be processed in current state",
                    Map.of("transfer_id", id.toString(), "status", status.name()));
        }
        this.status = TransferStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (status != TransferStatus.PROCESSING) {
            throw new ConflictException("Transfer cannot be completed in current state",
                    Map.of("transfer_id", id.toString(), "status", status.name()));
        }
        this.status = TransferStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void fail(String reason) {
        if (status != TransferStatus.PROCESSING && status != TransferStatus.QUEUED) {
            throw new ConflictException("Transfer cannot be failed in current state",
                    Map.of("transfer_id", id.toString(), "status", status.name()));
        }
        this.status = TransferStatus.FAILED;
        this.failedReason = reason;
        this.updatedAt = Instant.now();
    }

    public void approve(String subject) {
        if (status != TransferStatus.PENDING_APPROVAL) {
            throw new ConflictException("Transfer cannot be approved in current state",
                    Map.of("transfer_id", id.toString(), "status", status.name()));
        }
        // Legacy path: map approval to QUEUED (the orchestrator may later PROCESS/COMPLETE)
        this.status = TransferStatus.QUEUED;
        this.approvedBySubject = subject;
        this.updatedAt = Instant.now();
    }

    public void cancel(String subject, String reason) {
        if (status != TransferStatus.PENDING_APPROVAL && status != TransferStatus.QUEUED) {
            throw new ConflictException("Transfer cannot be canceled in current state",
                    Map.of("transfer_id", id.toString(), "status", status.name()));
        }
        this.status = TransferStatus.CANCELED;
        this.canceledBySubject = subject;
        this.reason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID id() { return id; }

    public MarketId marketId() { return marketId; }

    public OrgId orgId() { return orgId; }

    public LegalEntityId legalEntityId() { return legalEntityId; }

    public UUID sourceAccountId() { return sourceAccountId; }

    public UUID destinationAccountId() { return destinationAccountId; }

    public Money money() { return money; }

    public TransferType type() { return type; }

    public TransferStatus status() { return status; }

    public String reason() { return reason; }

    public String createdBySubject() { return createdBySubject; }

    public String approvedBySubject() { return approvedBySubject; }

    public String canceledBySubject() { return canceledBySubject; }

    public String failedReason() { return failedReason; }

    public Instant createdAt() { return createdAt; }

    public Instant updatedAt() { return updatedAt; }
}
