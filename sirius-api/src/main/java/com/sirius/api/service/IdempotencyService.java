package com.sirius.api.service;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sirius.core.exception.SiriusErrorCode;
import com.sirius.core.exception.SiriusException;
import com.sirius.data.entity.treasury.IdempotencyKeyEntity;
import com.sirius.data.repository.treasury.IdempotencyKeyJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private final IdempotencyKeyJpaRepository repository;
    private final ObjectMapper canonicalMapper;

    public IdempotencyService(IdempotencyKeyJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.canonicalMapper = objectMapper.copy()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    public Optional<IdempotencyKeyEntity> find(String marketId, UUID orgId, String key) {
        return repository.findByMarketIdAndOrgIdAndIdempotencyKey(marketId, orgId, key);
    }

    public String hashRequest(Object requestBody) {
        try {
            byte[] json = canonicalMapper.writeValueAsBytes(requestBody);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json));
        } catch (Exception e) {
            throw new SiriusException(SiriusErrorCode.INTERNAL_ERROR, "Unable to hash request", e);
        }
    }

    public <T> void store(String marketId, UUID orgId, String key, String requestHash, int status, T responseBody) {
        try {
            repository.save(IdempotencyKeyEntity.builder()
                    .marketId(marketId)
                    .orgId(orgId)
                    .idempotencyKey(key)
                    .requestHash(requestHash)
                    .responseStatus(status)
                    .responseBody(canonicalMapper.valueToTree(responseBody))
                    .build());
        } catch (Exception e) {
            throw new SiriusException(SiriusErrorCode.INTERNAL_ERROR, "Unable to store idempotency record", e);
        }
    }

    public ResponseEntity<String> replay(IdempotencyKeyEntity entity) {
        return ResponseEntity.status(entity.getResponseStatus())
                .header("X-Idempotent-Replay", "true")
                .body(entity.getResponseBody() == null ? null : entity.getResponseBody().toString());
    }

    public void assertSamePayloadOrThrow(IdempotencyKeyEntity entity, String requestHash) {
        if (!entity.getRequestHash().equals(requestHash)) {
            throw new SiriusException(SiriusErrorCode.IDEMPOTENCY_CONFLICT,
                    "Idempotency-Key reused with different payload");
        }
    }
}
