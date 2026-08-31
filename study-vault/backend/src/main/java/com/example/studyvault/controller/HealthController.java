package com.example.studyvault.controller;

import com.example.studyvault.dto.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health") public ApiResponse<Map<String, String>> health() { return ApiResponse.success(Map.of("status", "UP")); }
    @GetMapping("/ready") public ApiResponse<Map<String, String>> ready() { return ApiResponse.success(Map.of("status", "READY")); }
}
