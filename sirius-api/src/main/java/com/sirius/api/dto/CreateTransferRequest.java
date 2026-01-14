package com.sirius.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTransferRequest(
        @NotNull UUID source_account_id,
        @NotNull UUID destination_account_id,
        @NotBlank String legal_entity_id,
        @NotNull @Valid MoneyDto amount,
        String reason
) {
}
