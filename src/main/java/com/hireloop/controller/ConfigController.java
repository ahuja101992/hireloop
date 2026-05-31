package com.hireloop.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "http://localhost:3000")
public class ConfigController {
    private static final Map<String, Object> runtimeConfig = new ConcurrentHashMap<>();

    @Value("${apply-engine.enabled:false}")
    private boolean autoApplyEnabled;

    @Value("${apply-engine.headless:true}")
    private boolean headlessMode;

    public ConfigController() {
        runtimeConfig.put("autoApplyEnabled", false);
        runtimeConfig.put("headlessMode", true);
        runtimeConfig.put("emailPreferences", Map.of(
            "notifyNewJobs", true,
            "notifyResumeChanges", true,
            "notifyApplications", true,
            "digestFrequency", "immediate"
        ));
        runtimeConfig.put("targetCompanies", java.util.List.of(
            "Google", "Meta", "Apple", "Microsoft", "Amazon"
        ));
        runtimeConfig.put("jobFilters", Map.of(
            "minFitScore", 75,
            "maxJobAgeInDays", 7,
            "excludeKeywords", "contractor,temporary,part-time"
        ));
    }

    @GetMapping
    public Map<String, Object> getConfig() {
        return Map.of(
                "llm_provider", "claude",
                "version", "1.0.0"
        );
    }

    @GetMapping("/apply-engine")
    public Map<String, Object> getApplyEngineConfig() {
        return Map.of(
            "enabled", runtimeConfig.getOrDefault("autoApplyEnabled", false),
            "headless", runtimeConfig.getOrDefault("headlessMode", true)
        );
    }

    @PostMapping("/apply-engine")
    public Map<String, Object> updateApplyEngineConfig(@RequestBody Map<String, Boolean> config) {
        if (config.containsKey("enabled")) {
            runtimeConfig.put("autoApplyEnabled", config.get("enabled"));
        }
        if (config.containsKey("headless")) {
            runtimeConfig.put("headlessMode", config.get("headless"));
        }
        return getApplyEngineConfig();
    }

    @PostMapping("/filters")
    public Map<String, Object> updateFilters(@RequestBody Map<String, Object> filters) {
        runtimeConfig.put("filters", filters);
        return filters;
    }

    @PostMapping("/targets")
    public Map<String, Object> updateTargets(@RequestBody Map<String, Object> targets) {
        runtimeConfig.put("targets", targets);
        return targets;
    }

    @GetMapping("/email-preferences")
    public Map<String, Object> getEmailPreferences() {
        return (Map<String, Object>) runtimeConfig.getOrDefault("emailPreferences", Map.of(
            "notifyNewJobs", true,
            "notifyResumeChanges", true,
            "notifyApplications", true,
            "digestFrequency", "immediate"
        ));
    }

    @PostMapping("/email-preferences")
    public Map<String, Object> updateEmailPreferences(@RequestBody Map<String, Object> preferences) {
        runtimeConfig.put("emailPreferences", preferences);
        return getEmailPreferences();
    }

    @GetMapping("/target-companies")
    public Map<String, Object> getTargetCompanies() {
        return Map.of(
            "companies", runtimeConfig.getOrDefault("targetCompanies", java.util.List.of())
        );
    }

    @PostMapping("/target-companies")
    public Map<String, Object> updateTargetCompanies(@RequestBody Map<String, java.util.List<String>> data) {
        runtimeConfig.put("targetCompanies", data.get("companies"));
        return getTargetCompanies();
    }

    @GetMapping("/job-filters")
    public Map<String, Object> getJobFilters() {
        return (Map<String, Object>) runtimeConfig.getOrDefault("jobFilters", Map.of(
            "minFitScore", 75,
            "maxJobAgeInDays", 7,
            "excludeKeywords", ""
        ));
    }

    @PostMapping("/job-filters")
    public Map<String, Object> updateJobFilters(@RequestBody Map<String, Object> filters) {
        runtimeConfig.put("jobFilters", filters);
        return getJobFilters();
    }
}
