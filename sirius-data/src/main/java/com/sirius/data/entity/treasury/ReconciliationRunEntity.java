package com.sirius.data.entity.treasury;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_runs", indexes = {
        @Index(name = "idx_recon_scope", columnList = "market_id,org_id,legal_entity_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationRunEntity {

    public enum State {
        STARTED,
        RUNNING,
        COMPLETED,
        FAILED
    }

    public enum Status {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "market_id", nullable = false)
    private String marketId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "legal_entity_id", nullable = false)
    private UUID legalEntityId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "recon_status")
    private Status status;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, columnDefinition = "recon_state")
    private State state;

    @Column(name = "source_ref")
    private String sourceRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metrics;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (startedAt == null) startedAt = now;
        if (status == null) status = Status.PENDING;
        if (state == null) state = State.STARTED;
    }
}
