package com.sirius.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public final class SiriusTestcontainers {

    private SiriusTestcontainers() {
    }

    public static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("sirius")
                .withUsername("sirius")
                .withPassword("sirius");
    }

    public static GenericContainer<?> redis() {
        return new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379)
                .withCommand("redis-server --appendonly yes");
    }
}
