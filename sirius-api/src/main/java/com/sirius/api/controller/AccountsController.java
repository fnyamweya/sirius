package com.sirius.api.controller;

import com.sirius.api.dto.*;
import com.sirius.api.service.AccountBalanceService;
import com.sirius.api.tenant.SiriusRequestContext;
import com.sirius.api.tenant.SiriusRequestContextHolder;
import com.sirius.data.entity.treasury.AccountEntity;
import com.sirius.data.repository.treasury.AccountJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Accounts")
@RestController
@RequestMapping("/v1")
public class AccountsController {

    private final AccountJpaRepository accountRepo;
    private final AccountBalanceService balanceService;

    public AccountsController(AccountJpaRepository accountRepo, AccountBalanceService balanceService) {
        this.accountRepo = accountRepo;
        this.balanceService = balanceService;
    }

    @Operation(summary = "Fast account balance (cached)")
    @GetMapping(value = "/accounts/{id}/balance", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('TREASURY_VIEW','TREASURY_OPERATOR','TREASURY_APPROVER','TREASURY_ADMIN')")
    public AccountBalanceResponse balance(@PathVariable("id") UUID id) {
        SiriusRequestContext ctx = SiriusRequestContextHolder.getRequired();
        return balanceService.getCached(ctx.marketId().toString(), ctx.orgId().value(), id);
    }

    @Operation(summary = "Accounts query (read-only)")
    @PostMapping(value = "/accounts/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('TREASURY_VIEW','TREASURY_OPERATOR','TREASURY_APPROVER','TREASURY_ADMIN')")
    public Page<AccountResponse> query(@Valid @RequestBody AccountQueryRequest request) {
        SiriusRequestContext ctx = SiriusRequestContextHolder.getRequired();
        int pageNumber = request.page() == null ? 0 : request.page();
        int pageSize = request.size() == null ? 50 : request.size();
        PageRequest page = PageRequest.of(pageNumber, pageSize);

        Page<AccountEntity> result;
        if (request.external_ref_contains() != null && !request.external_ref_contains().isBlank()) {
            result = accountRepo.findByMarketIdAndOrgIdAndExternalRefContainingIgnoreCase(
                    ctx.marketId().toString(),
                    ctx.orgId().value(),
                    request.external_ref_contains(),
                    page
            );
        } else {
            String q = request.name_contains() == null ? "" : request.name_contains();
            result = accountRepo.findByMarketIdAndOrgIdAndNameContainingIgnoreCase(ctx.marketId().toString(), ctx.orgId().value(), q, page);
        }

        return result.map(a -> new AccountResponse(
                a.getId(),
                a.getMarketId(),
            a.getOrgId().toString(),
            a.getLegalEntityId().toString(),
                a.getCurrency(),
                a.getStatus(),
                a.getName(),
                a.getExternalRef()
        ));
    }
}
