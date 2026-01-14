package com.sirius.api.dto;

import java.util.UUID;

public record AccountBalanceResponse(
        UUID account_id,
        String currency,
        long available_minor,
        long ledger_minor
) {
}
