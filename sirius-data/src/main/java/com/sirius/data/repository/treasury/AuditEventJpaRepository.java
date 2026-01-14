package com.sirius.data.repository.treasury;

import com.sirius.data.entity.treasury.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, Long> {
}
