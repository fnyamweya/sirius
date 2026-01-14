package com.sirius.core.ledger;

import com.sirius.core.money.CurrencyCode;

import java.math.BigDecimal;
import java.util.UUID;

public record JournalLine(
        UUID id,
        UUID entryId,
        UUID accountId,
        CurrencyCode currency,
        LedgerDirection direction,
        JournalLineType lineType,
        BigDecimal amount,
        String memo
) {
}
