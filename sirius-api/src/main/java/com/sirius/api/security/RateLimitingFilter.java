package com.sirius.api.security;

import com.sirius.api.tenant.SiriusRequestContext;
import com.sirius.api.tenant.SiriusRequestContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final DefaultRedisScript<Long> TOKEN_BUCKET = new DefaultRedisScript<>(
            "local key_tokens = KEYS[1]\n" +
            "local key_ts = KEYS[2]\n" +
            "local now = tonumber(ARGV[1])\n" +
            "local capacity = tonumber(ARGV[2])\n" +
            "local refill_per_sec = tonumber(ARGV[3])\n" +
            "local ttl_ms = tonumber(ARGV[4])\n" +
            "local tokens = tonumber(redis.call('get', key_tokens) or capacity)\n" +
            "local last = tonumber(redis.call('get', key_ts) or now)\n" +
            "local delta = math.max(0, now - last)\n" +
            "local refill = (delta / 1000.0) * refill_per_sec\n" +
            "tokens = math.min(capacity, tokens + refill)\n" +
            "local allowed = 0\n" +
            "if tokens >= 1 then allowed = 1; tokens = tokens - 1 end\n" +
            "redis.call('set', key_tokens, tokens, 'px', ttl_ms)\n" +
            "redis.call('set', key_ts, now, 'px', ttl_ms)\n" +
            "return allowed\n",
            Long.class
    );

    private final StringRedisTemplate redis;

    @Value("${sirius.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${sirius.rate-limit.burst:60}")
    private long burst;

    @Value("${sirius.rate-limit.refill-per-second:10}")
    private double refillPerSecond;

    public RateLimitingFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled || request.getRequestURI().equals("/v1/health") || request.getRequestURI().equals("/v1/ready") || request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        SiriusRequestContext ctx;
        try {
            ctx = SiriusRequestContextHolder.getRequired();
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        String endpoint = request.getMethod() + " " + request.getRequestURI();
        String baseKey = "sirius:" + ctx.marketId() + ":" + ctx.orgId() + ":ratelimit:" + ctx.subject() + ":" + endpoint;

        long now = System.currentTimeMillis();
        long ttlMs = Duration.ofMinutes(5).toMillis();

        Long allowed = redis.execute(
                TOKEN_BUCKET,
                List.of(baseKey + ":tokens", baseKey + ":ts"),
                String.valueOf(now),
                String.valueOf(burst),
                String.valueOf(refillPerSecond),
                String.valueOf(ttlMs)
        );

        if (allowed == null || allowed == 0L) {
            response.setStatus(429);
            response.setHeader("Retry-After", "1");
            response.setContentType("application/problem+json");
            response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,\"code\":\"RATE_LIMITED\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
