package com.example.studyvault.controller;

import com.example.studyvault.dto.ApiResponse;
import java.util.Map;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
    private final HealthEndpoint healthEndpoint;

    public HealthController(HealthEndpoint healthEndpoint) { this.healthEndpoint = healthEndpoint; }
    /** Convenience constructor for lightweight controller tests. */
    public HealthController() { this(null); }

    @GetMapping("/health") public ApiResponse<Map<String, String>> health() { return ApiResponse.success(Map.of("status", "UP")); }
    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<Map<String, String>>> ready() {
        String dependencyStatus = "UP";
        if (healthEndpoint != null) {
            HealthComponent health = healthEndpoint.health();
            dependencyStatus = health == null || health.getStatus() == null ? "DOWN" : health.getStatus().getCode();
        }
        boolean ready = "UP".equalsIgnoreCase(dependencyStatus);
        Map<String, String> body = Map.of("status", ready ? "READY" : "NOT_READY");
        return ResponseEntity.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.success(body));
    }
}
