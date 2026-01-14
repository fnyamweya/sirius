package com.sirius.api.tenant;

import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;

import java.util.Set;

public record SiriusRequestContext(
        String correlationId,
        MarketId marketId,
        OrgId orgId,
        Set<LegalEntityId> allowedLegalEntities,
        String subject
) {
}
