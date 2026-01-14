package com.sirius.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelTransferRequest(
        @NotBlank String reason
) {
}
