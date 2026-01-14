package com.sirius.core.service;

import com.sirius.core.account.AccountStatus;
import com.sirius.core.exception.ConflictException;
import com.sirius.core.exception.ResourceNotFoundException;
import com.sirius.core.money.CurrencyCode;
import com.sirius.core.ledger.JournalEntry;
import com.sirius.core.ledger.JournalLine;
import com.sirius.core.ledger.JournalLineType;
import com.sirius.core.ledger.JournalStatus;
import com.sirius.core.ledger.LedgerDirection;
import com.sirius.core.port.*;
import com.sirius.core.security.RequestScope;
import com.sirius.core.transfer.Transfer;
import com.sirius.core.transfer.TransferStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;

public class TransferService {

    private final TransferStore transferStore;
    private final AccountStore accountStore;
    private final JournalStore journalStore;
    private final BalanceStore balanceStore;
    private final OutboxStore outboxStore;
    private final AuditStore auditStore;

    public TransferService(
            TransferStore transferStore,
            AccountStore accountStore,
            JournalStore journalStore,
            BalanceStore balanceStore,
            OutboxStore outboxStore,
            AuditStore auditStore
    ) {
        this.transferStore = transferStore;
        this.accountStore = accountStore;
        this.journalStore = journalStore;
        this.balanceStore = balanceStore;
        this.outboxStore = outboxStore;
        this.auditStore = auditStore;
    }

    public Transfer create(RequestScope scope, Transfer draft, String correlationId) {
        ensureLegalEntityAllowed(scope, draft.legalEntityId());

        AccountStore.AccountView source = accountStore.findById(scope.marketId(), scope.orgId(), draft.sourceAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found",
                        Map.of("account_id", draft.sourceAccountId().toString())));

        AccountStore.AccountView dest = accountStore.findById(scope.marketId(), scope.orgId(), draft.destinationAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found",
                        Map.of("account_id", draft.destinationAccountId().toString())));

        if (source.status() != AccountStatus.ACTIVE) {
            throw new ConflictException("Source account not active", Map.of("account_id", source.id().toString(), "status", source.status().name()));
        }
        if (dest.status() != AccountStatus.ACTIVE) {
            throw new ConflictException("Destination account not active", Map.of("account_id", dest.id().toString(), "status", dest.status().name()));
        }
        if (!source.legalEntityId().equals(draft.legalEntityId()) || !dest.legalEntityId().equals(draft.legalEntityId())) {
            throw new ConflictException("Accounts must belong to the same legal entity as the transfer",
                    Map.of("legal_entity_id", draft.legalEntityId().toString()));
        }
        if (!source.currency().equals(draft.money().currency()) || !dest.currency().equals(draft.money().currency())) {
            throw new ConflictException("Account currencies must match transfer currency",
                    Map.of("currency", draft.money().currency().toString()));
        }

        Transfer persisted = transferStore.save(draft);

        // Reserve funds at initiation (or fail fast).
        balanceStore.reserve(scope.marketId(), scope.orgId(), persisted.sourceAccountId(), persisted.money().amountMinor(), persisted.money().currency());

        outboxStore.add(new OutboxStore.OutboxRecord(
                UUID.randomUUID(),
                scope.marketId(),
                scope.orgId(),
                "Transfer",
                persisted.id(),
                "TransferCreated",
                "{\"transfer_id\":\"" + persisted.id() + "\"}",
                "transfer-created:" + persisted.id(),
                Instant.now(),
                null
        ));

        auditStore.write(
                scope.marketId(),
                scope.orgId(),
                persisted.legalEntityId(),
                correlationId,
                scope.subject(),
                "transfer.create",
                "transfer",
                persisted.id().toString(),
                "SUCCESS",
                Instant.now(),
                Map.of("amount_minor", persisted.money().amountMinor(), "currency", persisted.money().currency().toString())
        );

        return persisted;
    }

    public Transfer get(RequestScope scope, UUID transferId) {
        return transferStore.findById(scope.marketId(), scope.orgId(), transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found", Map.of("transfer_id", transferId.toString())));
    }

    public Transfer approve(RequestScope scope, UUID transferId, String correlationId) {
        Transfer transfer = get(scope, transferId);
        ensureLegalEntityAllowed(scope, transfer.legalEntityId());

        // 4-eyes: require approver != creator above a simple threshold (policy engine hook point).
        long fourEyesThresholdMinor = 1_000_000L;
        if (transfer.money().amountMinor() >= fourEyesThresholdMinor && scope.subject().equals(transfer.createdBySubject())) {
            throw new ConflictException("4-eyes approval required", Map.of("transfer_id", transfer.id().toString()));
        }

        transfer.approve(scope.subject());

        outboxStore.add(new OutboxStore.OutboxRecord(
            UUID.randomUUID(),
            scope.marketId(),
            scope.orgId(),
            "Transfer",
            transfer.id(),
            "TransferQueued",
            "{\"transfer_id\":\"" + transfer.id() + "\"}",
            "transfer-queued:" + transfer.id(),
            Instant.now(),
            null
        ));

        // Minimal synchronous processor for INTERNAL transfers.
        transfer.startProcessing();
        outboxStore.add(new OutboxStore.OutboxRecord(
            UUID.randomUUID(),
            scope.marketId(),
            scope.orgId(),
            "Transfer",
            transfer.id(),
            "TransferProcessing",
            "{\"transfer_id\":\"" + transfer.id() + "\"}",
            "transfer-processing:" + transfer.id(),
            Instant.now(),
            null
        ));

        CurrencyCode currency = transfer.money().currency();
        long amt = transfer.money().amountMinor();
        Instant now = Instant.now();

        byte[] prev = journalStore.findLatestHash(scope.marketId(), scope.orgId()).orElse(null);
        UUID entryId = UUID.randomUUID();
        BigDecimal amount = moneyToDecimal6(amt, currency);

        JournalLine debit = new JournalLine(
            UUID.randomUUID(),
            entryId,
            transfer.sourceAccountId(),
            currency,
            LedgerDirection.DEBIT,
            JournalLineType.PRINCIPAL,
            amount,
            "transfer " + transfer.id() + " principal"
        );

        JournalLine credit = new JournalLine(
            UUID.randomUUID(),
            entryId,
            transfer.destinationAccountId(),
            currency,
            LedgerDirection.CREDIT,
            JournalLineType.PRINCIPAL,
            amount,
            "transfer " + transfer.id() + " principal"
        );

        // Double-entry invariant per currency.
        ensureBalanced(List.of(debit, credit));

        byte[] entryHash = hashJournal(prev, scope.marketId().toString(), scope.orgId().toString(), correlationId, "transfer", transfer.id(), JournalStatus.POSTED, now, List.of(debit, credit));

        JournalEntry entry = new JournalEntry(
            entryId,
            scope.marketId(),
            scope.orgId(),
            correlationId,
            "transfer",
            transfer.id(),
            JournalStatus.POSTED,
            now,
            prev,
            entryHash
        );

        journalStore.appendPosted(entry, List.of(debit, credit));
        balanceStore.settle(scope.marketId(), scope.orgId(), transfer.sourceAccountId(), transfer.destinationAccountId(), amt, currency);

        transfer.complete();
        transferStore.update(transfer);

        outboxStore.add(new OutboxStore.OutboxRecord(
                UUID.randomUUID(),
                scope.marketId(),
                scope.orgId(),
                "Transfer",
                transfer.id(),
            "TransferCompleted",
                "{\"transfer_id\":\"" + transfer.id() + "\"}",
            "transfer-completed:" + transfer.id(),
                Instant.now(),
                null
        ));

        auditStore.write(
                scope.marketId(),
                scope.orgId(),
                transfer.legalEntityId(),
                correlationId,
                scope.subject(),
                "transfer.approve",
                "transfer",
                transfer.id().toString(),
                "SUCCESS",
                Instant.now(),
                Map.of("amount_minor", amt, "currency", currency.toString())
        );

        return transfer;
    }

    public Transfer cancel(RequestScope scope, UUID transferId, String reason, String correlationId) {
        Transfer transfer = get(scope, transferId);
        ensureLegalEntityAllowed(scope, transfer.legalEntityId());

        transfer.cancel(scope.subject(), reason);

        // Release reserved funds when canceling before settlement.
        if (transfer.status() == TransferStatus.CANCELED) {
            balanceStore.releaseReservation(scope.marketId(), scope.orgId(), transfer.sourceAccountId(), transfer.money().amountMinor(), transfer.money().currency());
        }
        transferStore.update(transfer);

        outboxStore.add(new OutboxStore.OutboxRecord(
                UUID.randomUUID(),
                scope.marketId(),
                scope.orgId(),
                "Transfer",
                transfer.id(),
                "TransferCanceled",
                "{\"transfer_id\":\"" + transfer.id() + "\"}",
                "transfer-canceled:" + transfer.id(),
                Instant.now(),
                null
        ));

        auditStore.write(
                scope.marketId(),
                scope.orgId(),
                transfer.legalEntityId(),
                correlationId,
                scope.subject(),
                "transfer.cancel",
                "transfer",
                transfer.id().toString(),
                "SUCCESS",
                Instant.now(),
                Map.of("reason", reason)
        );

        return transfer;
    }

    private static void ensureLegalEntityAllowed(RequestScope scope, com.sirius.core.tenant.LegalEntityId legalEntityId) {
        if (!scope.allowedLegalEntities().isEmpty() && !scope.allowedLegalEntities().contains(legalEntityId)) {
            throw new ConflictException("Subject not allowed for legal entity", Map.of("legal_entity_id", legalEntityId.toString()));
        }
    }

    private static void ensureBalanced(List<JournalLine> lines) {
        // Group by currency; for each, sum debits == sum credits.
        record Sums(BigDecimal debit, BigDecimal credit) {}

        java.util.Map<String, Sums> sums = new java.util.HashMap<>();
        for (JournalLine l : lines) {
            String ccy = l.currency().toString();
            Sums current = sums.getOrDefault(ccy, new Sums(BigDecimal.ZERO, BigDecimal.ZERO));
            if (l.direction() == LedgerDirection.DEBIT) {
                sums.put(ccy, new Sums(current.debit().add(l.amount()), current.credit()));
            } else {
                sums.put(ccy, new Sums(current.debit(), current.credit().add(l.amount())));
            }
        }
        for (var e : sums.entrySet()) {
            if (e.getValue().debit().compareTo(e.getValue().credit()) != 0) {
                throw new IllegalStateException("Unbalanced journal entry for currency " + e.getKey());
            }
        }
    }

    private static BigDecimal moneyToDecimal6(long amountMinor, CurrencyCode currency) {
        int fraction = java.util.Currency.getInstance(currency.toString()).getDefaultFractionDigits();
        // Normalize to scale 6 for storage; keep exact minor-unit precision.
        return BigDecimal.valueOf(amountMinor)
                .movePointLeft(fraction)
                .setScale(6, java.math.RoundingMode.UNNECESSARY);
    }

    private static byte[] hashJournal(
            byte[] prevHash,
            String marketId,
            String orgId,
            String correlationId,
            String referenceType,
            UUID referenceId,
            JournalStatus status,
            Instant postedAt,
            List<JournalLine> lines
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (prevHash != null) {
                digest.update(prevHash);
            }

            // Canonical header serialization.
            digest.update(marketId.getBytes(StandardCharsets.UTF_8));
            digest.update(orgId.getBytes(StandardCharsets.UTF_8));
            digest.update(correlationId.getBytes(StandardCharsets.UTF_8));
            digest.update(referenceType.getBytes(StandardCharsets.UTF_8));
            digest.update(referenceId.toString().getBytes(StandardCharsets.UTF_8));
            digest.update(status.name().getBytes(StandardCharsets.UTF_8));
            digest.update(postedAt.atZone(ZoneOffset.UTC).toInstant().toString().getBytes(StandardCharsets.UTF_8));

            // Canonical line ordering.
            lines.stream()
                    .sorted(java.util.Comparator.comparing((JournalLine l) -> l.accountId().toString())
                            .thenComparing(l -> l.direction().name())
                            .thenComparing(l -> l.lineType().name())
                            .thenComparing(l -> l.currency().toString())
                            .thenComparing(l -> l.amount().toPlainString()))
                    .forEach(l -> {
                        digest.update(l.accountId().toString().getBytes(StandardCharsets.UTF_8));
                        digest.update(l.direction().name().getBytes(StandardCharsets.UTF_8));
                        digest.update(l.lineType().name().getBytes(StandardCharsets.UTF_8));
                        digest.update(l.currency().toString().getBytes(StandardCharsets.UTF_8));
                        digest.update(l.amount().toPlainString().getBytes(StandardCharsets.UTF_8));
                        if (l.memo() != null) {
                            digest.update(l.memo().getBytes(StandardCharsets.UTF_8));
                        }
                    });
            return digest.digest();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash ledger entry", e);
        }
    }
}
