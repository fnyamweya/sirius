package com.sirius.core.transfer;

import com.sirius.core.exception.ConflictException;
import com.sirius.core.money.Money;
import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferStateMachineTest {

        private static final OrgId ORG_ID = OrgId.of("11111111-1111-1111-1111-111111111111");
        private static final LegalEntityId LEGAL_ENTITY_ID = LegalEntityId.of("22222222-2222-2222-2222-222222222222");

    @Test
    void happyPath_pending_to_queued_to_processing_to_completed() {
        Transfer t = Transfer.newPending(
                MarketId.of("KE"),
                ORG_ID,
                LEGAL_ENTITY_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.ofMinor(12_345, "KES"),
                "creator",
                "invoice-1"
        );

        assertThat(t.status()).isEqualTo(TransferStatus.PENDING_APPROVAL);

        t.approve("approver");
        assertThat(t.status()).isEqualTo(TransferStatus.QUEUED);

        t.startProcessing();
        assertThat(t.status()).isEqualTo(TransferStatus.PROCESSING);

        t.complete();
        assertThat(t.status()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void invalidTransitions_throw() {
        Transfer t = Transfer.newPending(
                MarketId.of("KE"),
                ORG_ID,
                LEGAL_ENTITY_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.ofMinor(100, "KES"),
                "creator",
                null
        );

        assertThatThrownBy(t::complete)
                .isInstanceOf(ConflictException.class);

        t.approve("approver");

        assertThatThrownBy(() -> t.approve("approver"))
                .isInstanceOf(ConflictException.class);

        t.startProcessing();

        assertThatThrownBy(() -> t.startProcessing())
                .isInstanceOf(ConflictException.class);

        t.complete();

        assertThatThrownBy(() -> t.cancel("any", "nope"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancel_allowed_from_pending_and_queued() {
        Transfer t1 = Transfer.newPending(
                MarketId.of("KE"),
                ORG_ID,
                LEGAL_ENTITY_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.ofMinor(100, "KES"),
                "creator",
                null
        );

        t1.cancel("creator", "changed mind");
        assertThat(t1.status()).isEqualTo(TransferStatus.CANCELED);

        Transfer t2 = Transfer.newPending(
                MarketId.of("KE"),
                ORG_ID,
                LEGAL_ENTITY_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.ofMinor(100, "KES"),
                "creator",
                null
        );

        t2.approve("approver");
        t2.cancel("creator", "cancel while queued");
        assertThat(t2.status()).isEqualTo(TransferStatus.CANCELED);
    }
}
