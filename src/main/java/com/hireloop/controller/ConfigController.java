package com.hireloop.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "http://localhost:3000")
public class ConfigController {

    @GetMapping
    public Map<String, Object> getConfig() {
        return Map.of(
                "llm_provider", "claude",
                "version", "1.0.0"
        );
    }

    @PostMapping("/filters")
    public Map<String, Object> updateFilters(@RequestBody Map<String, Object> filters) {
        // In production, persist these to a config file or database
        return filters;
    }

    @PostMapping("/targets")
    public Map<String, Object> updateTargets(@RequestBody Map<String, Object> targets) {
        // In production, persist these to a config file or database
        return targets;
    }
}
