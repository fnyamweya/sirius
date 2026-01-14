package com.sirius.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(
                classes = {SiriusApplication.class, TransfersIdempotencyIsolationIT.JwtTestConfig.class},
        properties = {
                "sirius.market.id=KE",
                "sirius.rate-limit.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
@AutoConfigureMockMvc
class TransfersIdempotencyIsolationIT {

        @TestConfiguration
        static class JwtTestConfig {
                @Bean
                @Primary
                JwtDecoder jwtDecoder() {
                        // Requests in this test use spring-security-test's jwt() post-processor, which bypasses
                        // actual token decoding. This bean exists only to satisfy Spring Security wiring.
                        return token -> {
                                throw new JwtException("JwtDecoder should not be called in TransfersIdempotencyIsolationIT");
                        };
                }
        }

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sirius")
            .withUsername("sirius")
            .withPassword("sirius");

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;

    private UUID sourceAccountId;
    private UUID destAccountId;
        private UUID orgId;
        private UUID legalEntityId;

    @BeforeEach
    void seedAccounts() {
        sourceAccountId = UUID.randomUUID();
        destAccountId = UUID.randomUUID();
                orgId = UUID.randomUUID();
                legalEntityId = UUID.randomUUID();

        jdbc.update("delete from outbox");
        jdbc.update("delete from idempotency_keys");
        jdbc.update("delete from transfers");
        jdbc.update("delete from account_balance");
        jdbc.update("delete from accounts");

        jdbc.update(
                "insert into accounts (id, market_id, org_id, legal_entity_id, currency, status, name, external_ref, row_version, created_at, updated_at) " +
                        "values (?, ?, ?, ?, ?, cast(? as account_status), ?, ?, 0, now(), now())",
                sourceAccountId,
                "KE",
                orgId,
                legalEntityId,
                "KES",
                "ACTIVE",
                "Source",
                "SRC"
        );

        jdbc.update(
                "insert into accounts (id, market_id, org_id, legal_entity_id, currency, status, name, external_ref, row_version, created_at, updated_at) " +
                        "values (?, ?, ?, ?, ?, cast(? as account_status), ?, ?, 0, now(), now())",
                destAccountId,
                "KE",
                orgId,
                legalEntityId,
                "KES",
                "ACTIVE",
                "Dest",
                "DST"
        );

        jdbc.update(
                "insert into account_balance (account_id, market_id, org_id, legal_entity_id, currency, available_minor, reserved_minor, pending_minor, ledger_minor, row_version, updated_at) " +
                        "values (?, ?, ?, ?, ?, ?, 0, 0, ?, 0, now())",
                sourceAccountId,
                "KE",
                orgId,
                legalEntityId,
                "KES",
                1_000_000L,
                1_000_000L
        );

        jdbc.update(
                "insert into account_balance (account_id, market_id, org_id, legal_entity_id, currency, available_minor, reserved_minor, pending_minor, ledger_minor, row_version, updated_at) " +
                        "values (?, ?, ?, ?, ?, 0, 0, 0, 0, 0, now())",
                destAccountId,
                "KE",
                orgId,
                legalEntityId,
                "KES"
        );
    }

    @Test
    void createTransfer_isIdempotent_andRejectsCrossMarket() throws Exception {
        String body = "{" +
                "\"source_account_id\":\"" + sourceAccountId + "\"," +
                "\"destination_account_id\":\"" + destAccountId + "\"," +
                "\"legal_entity_id\":\"" + legalEntityId + "\"," +
                "\"amount\":{\"amount_minor\":12345,\"currency\":\"KES\"}," +
                "\"reason\":\"invoice-123\"" +
                "}";

        var first = mvc.perform(
                        post("/v1/transfers")
                                .header("Idempotency-Key", "idem-1")
                                .contentType("application/json")
                                .content(body)
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TREASURY_OPERATOR")).jwt(j -> j
                                        .subject("user-1")
                                        .claim("market_id", "KE")
                                        .claim("org_id", orgId.toString())
                                        .claim("legal_entities", List.of(legalEntityId.toString()))
                                        .claim("roles", List.of("TREASURY_OPERATOR"))
                                ))
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(first.getResponse().getContentAsString());
        assertThat(firstJson.get("id").asText()).isNotBlank();

        var replay = mvc.perform(
                        post("/v1/transfers")
                                .header("Idempotency-Key", "idem-1")
                                .contentType("application/json")
                                .content(body)
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TREASURY_OPERATOR")).jwt(j -> j
                                        .subject("user-1")
                                        .claim("market_id", "KE")
                                        .claim("org_id", orgId.toString())
                                        .claim("legal_entities", List.of(legalEntityId.toString()))
                                        .claim("roles", List.of("TREASURY_OPERATOR"))
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Idempotent-Replay", "true"))
                .andReturn();

        JsonNode replayJson = objectMapper.readTree(replay.getResponse().getContentAsString());
        assertThat(replayJson.get("id").asText()).isEqualTo(firstJson.get("id").asText());
        assertThat(replayJson.get("status").asText()).isEqualTo(firstJson.get("status").asText());

        mvc.perform(
                        post("/v1/transfers")
                                .header("Idempotency-Key", "idem-cross-market")
                                .contentType("application/json")
                                .content(body)
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TREASURY_OPERATOR")).jwt(j -> j
                                        .subject("user-1")
                                        .claim("market_id", "UG")
                                                                                .claim("org_id", orgId.toString())
                                                                                .claim("legal_entities", List.of(legalEntityId.toString()))
                                        .claim("roles", List.of("TREASURY_OPERATOR"))
                                ))
                )
                .andExpect(status().isForbidden());
    }
}
