package com.sirius.api.dto;

import java.util.List;

public record PageResponse<T>(
        int page,
        int size,
        List<T> items
) {
}
