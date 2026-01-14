package com.sirius.api.config;

import com.sirius.core.port.*;
import com.sirius.core.service.TransferService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TreasuryServiceConfig {

    @Bean
    public TransferService transferService(
            TransferStore transferStore,
            AccountStore accountStore,
            JournalStore journalStore,
            BalanceStore balanceStore,
            OutboxStore outboxStore,
            AuditStore auditStore
    ) {
        return new TransferService(transferStore, accountStore, journalStore, balanceStore, outboxStore, auditStore);
    }
}
