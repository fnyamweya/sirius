package com.sirius.core.transfer;

public enum TransferStatus {
    DRAFT,
    PENDING_APPROVAL,
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    /**
     * Backward-compatible alias (legacy state). Prefer COMPLETED.
     */
    APPROVED,
    CANCELED
}
