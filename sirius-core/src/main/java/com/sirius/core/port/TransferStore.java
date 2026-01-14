package com.sirius.core.port;

import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;
import com.sirius.core.transfer.Transfer;

import java.util.Optional;
import java.util.UUID;

public interface TransferStore {

    Transfer save(Transfer transfer);

    Optional<Transfer> findById(MarketId marketId, OrgId orgId, UUID transferId);

    void update(Transfer transfer);
}
