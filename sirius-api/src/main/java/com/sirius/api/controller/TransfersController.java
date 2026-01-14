package com.sirius.api.controller;

import com.sirius.api.dto.CancelTransferRequest;
import com.sirius.api.dto.CreateTransferRequest;
import com.sirius.api.dto.MoneyDto;
import com.sirius.api.dto.TransferResponse;
import com.sirius.api.service.IdempotencyService;
import com.sirius.api.tenant.SiriusRequestContext;
import com.sirius.api.tenant.SiriusRequestContextHolder;
import com.sirius.core.money.Money;
import com.sirius.core.security.RequestScope;
import com.sirius.core.service.TransferService;
import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.transfer.Transfer;
import com.sirius.data.entity.treasury.IdempotencyKeyEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Transfers")
@RestController
@RequestMapping("/v1/transfers")
public class TransfersController {

    private final TransferService transferService;
    private final IdempotencyService idempotencyService;

    public TransfersController(TransferService transferService, IdempotencyService idempotencyService) {
        this.transferService = transferService;
        this.idempotencyService = idempotencyService;
    }

    @Operation(summary = "Idempotent create transfer")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('TREASURY_OPERATOR')")
    @Transactional
    public ResponseEntity<?> create(
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateTransferRequest request
    ) {
        SiriusRequestContext ctx = SiriusRequestContextHolder.getRequired();
        String requestHash = idempotencyService.hashRequest(request);

        var existing = idempotencyService.find(ctx.marketId().toString(), ctx.orgId().value(), idempotencyKey);
        if (existing.isPresent()) {
            idempotencyService.assertSamePayloadOrThrow(existing.get(), requestHash);
            return idempotencyService.replay(existing.get());
        }

        Transfer created = transferService.create(
            RequestScope.of(ctx.marketId(), ctx.orgId(), ctx.allowedLegalEntities(), ctx.subject()),
            Transfer.newPending(
                ctx.marketId(),
                ctx.orgId(),
                LegalEntityId.of(request.legal_entity_id()),
                request.source_account_id(),
                request.destination_account_id(),
                Money.ofMinor(request.amount().amount_minor(), request.amount().currency()),
                ctx.subject(),
                request.reason()
            ),
            ctx.correlationId()
        );

        TransferResponse body = toResponse(created);
        idempotencyService.store(ctx.marketId().toString(), ctx.orgId().value(), idempotencyKey, requestHash, 201, body);
        return ResponseEntity.status(201).body(body);
    }

    @Operation(summary = "Get transfer")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('TREASURY_VIEW','TREASURY_OPERATOR','TREASURY_APPROVER','TREASURY_ADMIN')")
    public TransferResponse get(@PathVariable("id") UUID id) {
        SiriusRequestContext ctx = SiriusRequestContextHolder.getRequired();
        Transfer transfer = transferService.get(RequestScope.of(ctx.marketId(), ctx.orgId(), ctx.allowedLegalEntities(), ctx.subject()), id);
        return toResponse(transfer);
    }

    @Operation(summary = "Approve transfer")
    @PostMapping(value = "/{id}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('TREASURY_APPROVER')")
    @Transactional
    public TransferResponse approve(@PathVariable("id") UUID id) {
        SiriusRequestContext ctx = SiriusRequestContextHolder.getRequired();
        Transfer transfer = transferService.approve(RequestScope.of(ctx.marketId(), ctx.orgId(), ctx.allowedLegalEntities(), ctx.subject()), id, ctx.correlationId());
        return toResponse(transfer);
    }

    @Operation(summary = "Cancel transfer")
    @PostMapping(value = "/{id}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('TREASURY_OPERATOR')")
    @Transactional
    public TransferResponse cancel(@PathVariable("id") UUID id, @Valid @RequestBody CancelTransferRequest request) {
        SiriusRequestContext ctx = SiriusRequestContextHolder.getRequired();
        Transfer transfer = transferService.cancel(RequestScope.of(ctx.marketId(), ctx.orgId(), ctx.allowedLegalEntities(), ctx.subject()), id, request.reason(), ctx.correlationId());
        return toResponse(transfer);
    }

    private static TransferResponse toResponse(Transfer t) {
        return new TransferResponse(
                t.id(),
                t.marketId().toString(),
                t.orgId().toString(),
                t.legalEntityId().toString(),
                t.sourceAccountId(),
                t.destinationAccountId(),
                new MoneyDto(t.money().amountMinor(), t.money().currency().toString()),
                t.status(),
                t.reason(),
                t.createdAt(),
                t.updatedAt()
        );
    }
}
