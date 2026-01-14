package com.sirius.api.security;

import com.sirius.api.config.SiriusSecurityProperties;
import com.sirius.api.tenant.SiriusRequestContext;
import com.sirius.api.tenant.SiriusRequestContextHolder;
import com.sirius.core.tenant.LegalEntityId;
import com.sirius.core.tenant.MarketId;
import com.sirius.core.tenant.OrgId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
public class SiriusRequestContextFilter extends OncePerRequestFilter {

    private final SiriusSecurityProperties properties;

    public SiriusRequestContextFilter(SiriusSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = correlationId(request);
        MDC.put("correlation_id", correlationId);

        try {
            if (request.getRequestURI().equals("/v1/health") || request.getRequestURI().equals("/v1/ready") || request.getRequestURI().startsWith("/actuator")) {
                filterChain.doFilter(request, response);
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
                filterChain.doFilter(request, response);
                return;
            }

            Jwt jwt = jwtAuth.getToken();
            String marketClaim = properties.getSecurity().getClaims().getMarketId();
            String orgClaim = properties.getSecurity().getClaims().getOrgId();
            String leClaim = properties.getSecurity().getClaims().getLegalEntities();

            String configuredMarket = properties.getMarket().getId();
            if (configuredMarket == null || configuredMarket.isBlank()) {
                writeProblem(response, 500, "INTERNAL_ERROR", "SIRIUS_MARKET_ID is required", correlationId);
                return;
            }

            String marketId;
            String orgId;
            try {
                marketId = claimAsString(jwt, marketClaim);
                orgId = claimAsString(jwt, orgClaim);
            } catch (IllegalArgumentException e) {
                writeProblem(response, 401, "UNAUTHORIZED", e.getMessage(), correlationId);
                return;
            }

            if (!configuredMarket.equals(marketId)) {
                writeProblem(response, 403, "FORBIDDEN", "Cross-market access forbidden", correlationId);
                return;
            }
            Set<LegalEntityId> allowedLegalEntities = claimAsStringSet(jwt, leClaim).stream().map(LegalEntityId::of).collect(java.util.stream.Collectors.toUnmodifiableSet());

            SiriusRequestContextHolder.set(new SiriusRequestContext(
                    correlationId,
                    MarketId.of(marketId),
                    OrgId.of(orgId),
                    allowedLegalEntities,
                    jwt.getSubject()
            ));

            MDC.put("market_id", marketId);
            MDC.put("org_id", orgId);
            MDC.put("subject", jwt.getSubject());

            filterChain.doFilter(request, response);
        } finally {
            SiriusRequestContextHolder.clear();
            MDC.remove("correlation_id");
            MDC.remove("market_id");
            MDC.remove("org_id");
            MDC.remove("subject");
        }
    }

    private static String correlationId(HttpServletRequest request) {
        String incoming = request.getHeader("X-Correlation-Id");
        if (incoming != null && !incoming.isBlank()) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString();
    }

    private static String claimAsString(Jwt jwt, String claimName) {
        Object val = jwt.getClaim(claimName);
        if (val == null) {
            throw new IllegalArgumentException("Missing required claim: " + claimName);
        }
        return String.valueOf(val);
    }

    private static void writeProblem(HttpServletResponse response, int status, String code, String detail, String correlationId) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":" + json(statusTitle(status)) + ",\"status\":" + status +
                        ",\"detail\":" + json(detail) + ",\"code\":" + json(code) + ",\"correlation_id\":" + json(correlationId) + "}"
        );
    }

    private static String statusTitle(int status) {
        return switch (status) {
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            default -> "Error";
        };
    }

    private static String json(String s) {
        if (s == null) return "null";
        String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    @SuppressWarnings("unchecked")
    private static Set<String> claimAsStringSet(Jwt jwt, String claimName) {
        Object val = jwt.getClaim(claimName);
        if (val == null) {
            return Set.of();
        }
        if (val instanceof Collection<?> c) {
            Set<String> out = new HashSet<>();
            for (Object o : c) {
                out.add(String.valueOf(o));
            }
            return out;
        }
        if (val instanceof String s && !s.isBlank()) {
            return Set.of(s);
        }
        return Set.of();
    }
}
