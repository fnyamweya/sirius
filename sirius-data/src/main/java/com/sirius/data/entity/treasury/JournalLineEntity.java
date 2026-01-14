package com.sirius.data.entity.treasury;

import com.sirius.core.ledger.JournalLineType;
import com.sirius.core.ledger.LedgerDirection;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journal_lines", indexes = {
        @Index(name = "idx_journal_lines_entry_id", columnList = "entry_id"),
        @Index(name = "idx_journal_lines_account_posted", columnList = "account_id,created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalLineEntity {

    @Id
    private UUID id;

    @Column(name = "entry_id", nullable = false)
    private UUID entryId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ledger_direction")
    private LedgerDirection direction;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "line_type", nullable = false, columnDefinition = "journal_line_type")
    private JournalLineType lineType;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal amount;

    @Column
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
