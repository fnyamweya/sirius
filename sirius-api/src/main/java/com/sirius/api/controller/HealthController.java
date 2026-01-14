package com.sirius.api.controller;

import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    
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
    
    @Data
    static class HealthResponse {
        private String status;
        private Instant timestamp;
        private String service;
        private String version;
        private Map<String, String> components;
    }
}
