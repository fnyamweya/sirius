package com.sirius.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Base class for all domain entities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {
    
    private Long id;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
