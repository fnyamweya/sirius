package com.sirius.data.entity.treasury;

import com.sirius.core.account.AccountStatus;
import com.sirius.core.account.AccountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_scope", columnList = "market_id,org_id,legal_entity_id"),
        @Index(name = "idx_accounts_external_ref", columnList = "market_id,org_id,external_ref")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "market_id", nullable = false)
    private String marketId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "legal_entity_id", nullable = false)
    private UUID legalEntityId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, columnDefinition = "account_type")
    private AccountType accountType;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "account_status")
    private AccountStatus status;

    @Column(name = "allow_overdraft", nullable = false)
    private boolean allowOverdraft;

    @Column(nullable = false)
    private String name;

    @Column(name = "external_ref")
    private String externalRef;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (accountType == null) accountType = AccountType.OPERATING;
        if (status == null) status = AccountStatus.ACTIVE;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
