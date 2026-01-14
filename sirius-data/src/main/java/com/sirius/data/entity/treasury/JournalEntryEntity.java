package com.sirius.data.entity.treasury;

import com.sirius.core.ledger.JournalStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_entries", indexes = {
        @Index(name = "idx_journal_entries_scope_posted", columnList = "market_id,org_id,posted_at"),
        @Index(name = "idx_journal_entries_reference", columnList = "reference_type,reference_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntryEntity {

    @Id
    private UUID id;

    @Column(name = "market_id", nullable = false)
    private String marketId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "reference_type", nullable = false)
    private String referenceType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "journal_status")
    private JournalStatus status;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @Column(name = "prev_hash")
    private byte[] prevHash;

    @Column(name = "entry_hash", nullable = false)
    private byte[] entryHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (postedAt == null) postedAt = createdAt;
        if (status == null) status = JournalStatus.POSTED;
    }
}
