package com.sirius.data.entity.treasury;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_balance", indexes = {
        @Index(name = "idx_account_balance_scope", columnList = "market_id,org_id,legal_entity_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalanceEntity {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "market_id", nullable = false)
    private String marketId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "legal_entity_id", nullable = false)
    private UUID legalEntityId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency;

    @Column(name = "available_minor", nullable = false)
    private long availableMinor;

    @Column(name = "reserved_minor", nullable = false)
    private long reservedMinor;

    @Column(name = "pending_minor", nullable = false)
    private long pendingMinor;

    @Column(name = "ledger_minor", nullable = false)
    private long ledgerMinor;

    @Column(name = "as_of_entry_id")
    private UUID asOfEntryId;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
