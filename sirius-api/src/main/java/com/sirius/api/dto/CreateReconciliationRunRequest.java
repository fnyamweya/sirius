package com.sirius.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateReconciliationRunRequest(
        @NotBlank String legal_entity_id
) {
}
