package com.sirius.data.entity.treasury;

import com.sirius.core.transfer.TransferStatus;
import com.sirius.core.transfer.TransferType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "idx_transfers_scope", columnList = "market_id,org_id,legal_entity_id"),
        @Index(name = "idx_transfers_status", columnList = "market_id,org_id,status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "market_id", nullable = false)
    private String marketId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "legal_entity_id", nullable = false)
    private UUID legalEntityId;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false, columnDefinition = "transfer_type")
    private TransferType transferType;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "transfer_status")
    private TransferStatus status;

    @Column
    private String reason;

    @Column(name = "created_by_subject", nullable = false)
    private String createdBySubject;

    @Column(name = "approved_by_subject")
    private String approvedBySubject;

    @Column(name = "canceled_by_subject")
    private String canceledBySubject;

    @Column(name = "failed_reason")
    private String failedReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "queued_at")
    private Instant queuedAt;

    @Column(name = "processing_at")
    private Instant processingAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = TransferStatus.PENDING_APPROVAL;
        if (transferType == null) transferType = TransferType.INTERNAL;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
