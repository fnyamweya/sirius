package com.sirius.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record MoneyDto(
        @Positive long amount_minor,
        @NotBlank String currency
) {
}
