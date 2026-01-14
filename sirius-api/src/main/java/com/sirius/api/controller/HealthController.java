package com.sirius.api.controller;

import lombok.Data;
import org.flywaydb.core.Flyway;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;
    private final Flyway flyway;

    public HealthController(JdbcTemplate jdbcTemplate, RedisConnectionFactory redisConnectionFactory, Flyway flyway) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
        this.flyway = flyway;
    }
    
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse();
        response.setStatus("UP");
        response.setTimestamp(Instant.now());
        response.setService("Sirius Treasury Platform");
        response.setVersion("1.0.0");
        
        Map<String, String> components = new HashMap<>();
        components.put("database", "UP");
        components.put("redis", "UP");
        response.setComponents(components);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ready")
    public ResponseEntity<HealthResponse> ready() {
        HealthResponse response = new HealthResponse();
        response.setTimestamp(Instant.now());
        response.setService("Sirius Treasury Platform");
        response.setVersion("1.0.0");

        Map<String, String> components = new HashMap<>();
        boolean ok = true;

        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            components.put("database", "UP");
        } catch (Exception e) {
            ok = false;
            components.put("database", "DOWN");
        }

        try {
            var conn = redisConnectionFactory.getConnection();
            try {
                String pong = conn.ping();
                components.put("redis", "PONG".equalsIgnoreCase(pong) ? "UP" : "DEGRADED");
                if (!"PONG".equalsIgnoreCase(pong)) {
                    ok = false;
                }
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            ok = false;
            components.put("redis", "DOWN");
        }

        try {
            flyway.validate();
            components.put("migrations", "UP");
        } catch (Exception e) {
            ok = false;
            components.put("migrations", "DOWN");
        }

        response.setComponents(components);
        response.setStatus(ok ? "READY" : "NOT_READY");
        return ResponseEntity.status(ok ? 200 : 503).body(response);
    }
    
    @Data
    static class HealthResponse {
        private String status;
        private Instant timestamp;
        private String service;
        private String version;
        private Map<String, String> components;
    }
}
