package com.sirius.core.port;

import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;

import java.time.Instant;
import java.util.Map;

public interface AuditStore {

    void write(
            MarketId marketId,
            OrgId orgId,
            LegalEntityId legalEntityId,
            String correlationId,
            String subject,
            String action,
            String resourceType,
            String resourceId,
            String outcome,
            Instant at,
            Map<String, Object> metadata
    );
}
