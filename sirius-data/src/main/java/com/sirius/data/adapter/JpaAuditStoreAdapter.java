package com.sirius.data.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirius.core.port.AuditStore;
import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;
import com.sirius.data.entity.treasury.AuditEventEntity;
import com.sirius.data.repository.treasury.AuditEventJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class JpaAuditStoreAdapter implements AuditStore {

    private final AuditEventJpaRepository auditEventJpaRepository;
    private final ObjectMapper objectMapper;

    public JpaAuditStoreAdapter(AuditEventJpaRepository auditEventJpaRepository, ObjectMapper objectMapper) {
        this.auditEventJpaRepository = auditEventJpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void write(MarketId marketId, OrgId orgId, LegalEntityId legalEntityId, String correlationId, String subject, String action,
                      String resourceType, String resourceId, String outcome, Instant at, Map<String, Object> metadata) {
        JsonNode json = null;
        try {
            if (metadata != null && !metadata.isEmpty()) {
                json = objectMapper.valueToTree(metadata);
            }
        } catch (Exception ignored) {
            json = null;
        }

        auditEventJpaRepository.save(AuditEventEntity.builder()
                .marketId(marketId.toString())
            .orgId(orgId.value())
            .legalEntityId(legalEntityId == null ? null : legalEntityId.value())
                .correlationId(correlationId)
                .subject(subject)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .outcome(outcome)
                .createdAt(at == null ? Instant.now() : at)
                .metadata(json)
                .build());
    }
}
