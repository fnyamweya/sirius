package com.sirius.data.entity.treasury;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event", indexes = {
        @Index(name = "idx_audit_scope", columnList = "market_id,org_id,created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "market_id", nullable = false)
    private String marketId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "legal_entity_id")
    private UUID legalEntityId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String action;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(nullable = false)
    private String outcome;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
