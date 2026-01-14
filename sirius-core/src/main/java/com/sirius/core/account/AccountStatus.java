package com.sirius.core.account;

public enum AccountStatus {
    ACTIVE,
    FROZEN,
    /**
     * Backward-compatible alias for FROZEN (legacy enum value).
     */
    SUSPENDED,
    CLOSED
}
