package com.sirius.core.security;

import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;

import java.util.Set;

public record RequestScope(
        MarketId marketId,
        OrgId orgId,
        Set<LegalEntityId> allowedLegalEntities,
        String subject
) {

    public static RequestScope of(MarketId marketId, OrgId orgId, Set<LegalEntityId> allowedLegalEntities, String subject) {
        if (marketId == null) throw new IllegalArgumentException("marketId is required");
        if (orgId == null) throw new IllegalArgumentException("orgId is required");
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("subject is required");
        Set<LegalEntityId> normalizedAllowed = allowedLegalEntities == null ? Set.of() : Set.copyOf(allowedLegalEntities);
        return new RequestScope(marketId, orgId, normalizedAllowed, subject);
    }
}
