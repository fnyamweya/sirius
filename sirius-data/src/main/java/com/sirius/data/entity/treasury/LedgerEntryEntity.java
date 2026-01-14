package com.sirius.data.entity.treasury;

import com.sirius.core.ledger.LedgerDirection;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_scope", columnList = "market_id,org_id,legal_entity_id"),
        @Index(name = "idx_ledger_account", columnList = "market_id,org_id,account_id,occurred_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "market_id", nullable = false)
    private String marketId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "legal_entity_id", nullable = false)
    private UUID legalEntityId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "transfer_id")
    private UUID transferId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ledger_direction")
    private LedgerDirection direction;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "prev_hash")
    private byte[] prevHash;

    @Column(name = "entry_hash", nullable = false)
    private byte[] entryHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
