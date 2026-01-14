package com.sirius.api.controller;

import com.sirius.api.dto.LedgerEntriesQueryRequest;
import com.sirius.api.dto.LedgerEntryResponse;
import com.sirius.api.dto.PageResponse;
import com.sirius.api.service.LedgerQueryService;
import com.sirius.api.tenant.SiriusRequestContext;
import com.sirius.api.tenant.SiriusRequestContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Ledger")
@RestController
@RequestMapping("/v1/ledger")
public class LedgerController {

    private final LedgerQueryService ledgerQueryService;

    public LedgerController(LedgerQueryService ledgerQueryService) {
        this.ledgerQueryService = ledgerQueryService;
    }

    @Operation(summary = "Ledger entries query (read-only)")
    @PostMapping(value = "/entries/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('TREASURY_VIEW','TREASURY_OPERATOR','TREASURY_APPROVER','TREASURY_ADMIN')")
    public PageResponse<LedgerEntryResponse> query(@Valid @RequestBody LedgerEntriesQueryRequest request) {
        SiriusRequestContext ctx = SiriusRequestContextHolder.getRequired();
        List<LedgerEntryResponse> items = ledgerQueryService.query(ctx.marketId().toString(), ctx.orgId().value(), request);
        int pageNumber = request.page() == null ? 0 : request.page();
        int pageSize = request.size() == null ? 100 : request.size();
        return new PageResponse<>(pageNumber, pageSize, items);
    }
}
