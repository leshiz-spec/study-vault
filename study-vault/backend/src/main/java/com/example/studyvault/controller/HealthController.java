package com.example.studyvault.controller;

import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health") public Map<String, Object> health() { return response("UP"); }
    @GetMapping("/ready") public Map<String, Object> ready() { return response("READY"); }
    private Map<String, Object> response(String status) { var result = new LinkedHashMap<String, Object>(); result.put("success", true); result.put("data", Map.of("status", status)); result.put("error", null); return result; }
}
