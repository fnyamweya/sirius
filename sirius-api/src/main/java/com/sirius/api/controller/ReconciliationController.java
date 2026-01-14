package com.sirius.api.controller;

import com.sirius.api.dto.CreateReconciliationRunRequest;
import com.sirius.api.dto.ReconciliationRunResponse;
import com.sirius.api.tenant.SiriusRequestContext;
import com.sirius.api.tenant.SiriusRequestContextHolder;
import com.sirius.core.exception.ResourceNotFoundException;
import com.sirius.data.entity.treasury.ReconciliationRunEntity;
import com.sirius.data.repository.treasury.ReconciliationRunJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Reconciliation")
@RestController
@RequestMapping("/v1/reconciliation")
public class ReconciliationController {

    private final ReconciliationRunJpaRepository repo;
    private final StringRedisTemplate redis;

    public ReconciliationController(ReconciliationRunJpaRepository repo, StringRedisTemplate redis) {
        this.repo = repo;
        this.redis = redis;
    }

    @Operation(summary = "Start reconciliation job")
    @PostMapping(value = "/runs", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('TREASURY_OPERATOR')")
    @Transactional
    public ReconciliationRunResponse start(@Valid @RequestBody CreateReconciliationRunRequest request) {
        SiriusRequestContext ctx = SiriusRequestContextHolder.getRequired();
        UUID legalEntityId = UUID.fromString(request.legal_entity_id());

        ReconciliationRunEntity entity = repo.save(ReconciliationRunEntity.builder()
                .marketId(ctx.marketId().toString())
            .orgId(ctx.orgId().value())
            .legalEntityId(legalEntityId)
                .status(ReconciliationRunEntity.Status.PENDING)
                .startedAt(Instant.now())
                .build());

        String stream = "sirius:" + ctx.marketId() + ":" + ctx.orgId() + ":reconciliation:requests";
        redis.opsForStream().add(MapRecord.create(stream, Map.of(
                "recon_run_id", entity.getId().toString(),
                "market_id", ctx.marketId().toString(),
                "org_id", ctx.orgId().toString(),
                "legal_entity_id", request.legal_entity_id()
        )));

        return toResponse(entity);
    }

    @Operation(summary = "Get reconciliation run")
    @GetMapping(value = "/runs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('TREASURY_VIEW','TREASURY_OPERATOR','TREASURY_APPROVER','TREASURY_ADMIN')")
    public ReconciliationRunResponse get(@PathVariable("id") UUID id) {
        SiriusRequestContext ctx = SiriusRequestContextHolder.getRequired();
        ReconciliationRunEntity entity = repo.findByMarketIdAndOrgIdAndId(ctx.marketId().toString(), ctx.orgId().value(), id)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation run not found", Map.of("id", id.toString())));
        return toResponse(entity);
    }

    private static ReconciliationRunResponse toResponse(ReconciliationRunEntity e) {
        return new ReconciliationRunResponse(
                e.getId(),
                e.getMarketId(),
                e.getOrgId().toString(),
                e.getLegalEntityId().toString(),
                e.getStatus().name(),
                e.getStartedAt(),
                e.getCompletedAt()
        );
    }
}
