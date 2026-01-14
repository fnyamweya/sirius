package com.sirius.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirius.api.dto.AccountBalanceResponse;
import com.sirius.core.exception.ResourceNotFoundException;
import com.sirius.data.entity.treasury.AccountBalanceEntity;
import com.sirius.data.repository.treasury.AccountBalanceJpaRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountBalanceService {

    private final AccountBalanceJpaRepository repo;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public AccountBalanceService(AccountBalanceJpaRepository repo, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.repo = repo;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public AccountBalanceResponse getCached(String marketId, UUID orgId, UUID accountId) {
        String key = cacheKey(marketId, orgId, accountId);
        String cached = redis.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, AccountBalanceResponse.class);
            } catch (Exception ignored) {
            }
        }

        String lockKey = key + ":lock";
        boolean leader = Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(2)));
        try {
            if (!leader) {
                AccountBalanceResponse direct = load(marketId, orgId, accountId);
                write(key, direct);
                return direct;
            }

            AccountBalanceResponse fresh = load(marketId, orgId, accountId);
            write(key, fresh);
            return fresh;
        } finally {
            if (leader) {
                redis.delete(lockKey);
            }
        }
    }

    private AccountBalanceResponse load(String marketId, UUID orgId, UUID accountId) {
        AccountBalanceEntity entity = repo.findByMarketIdAndOrgIdAndAccountId(marketId, orgId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account balance not found", Map.of("account_id", accountId.toString())));
        return new AccountBalanceResponse(entity.getAccountId(), entity.getCurrency(), entity.getAvailableMinor(), entity.getLedgerMinor());
    }

    private void write(String key, AccountBalanceResponse value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            long jitterSeconds = ThreadLocalRandom.current().nextLong(0, 30);
            redis.opsForValue().set(key, json, Duration.ofSeconds(60 + jitterSeconds));
        } catch (Exception ignored) {
        }
    }

    private static String cacheKey(String marketId, UUID orgId, UUID accountId) {
        return "sirius:" + marketId + ":" + orgId + ":account-balance:" + accountId + ":v1";
    }
}
