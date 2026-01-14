package com.sirius.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntriesQueryRequest(
        UUID account_id,
        UUID transfer_id,
        Instant from,
        Instant to,
        String direction,
        @Min(0) Integer page,
        @Min(1) @Max(500) Integer size
) {
}
