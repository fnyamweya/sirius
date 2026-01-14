package com.sirius.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AccountQueryRequest(
        String name_contains,
        String external_ref_contains,
        @Min(0) Integer page,
        @Min(1) @Max(200) Integer size
) {
}
