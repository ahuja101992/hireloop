package com.hireloop.controller;

import com.hireloop.model.PrepReadiness;
import com.hireloop.repository.PrepReadinessRepository;
import com.hireloop.service.PrepTrackerService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/readiness")
@CrossOrigin(origins = "http://localhost:3000")
public class ReadinessController {
    private final PrepReadinessRepository prepRepository;
    private final PrepTrackerService prepTrackerService;

    public ReadinessController(
            PrepReadinessRepository prepRepository,
            PrepTrackerService prepTrackerService) {
        this.prepRepository = prepRepository;
        this.prepTrackerService = prepTrackerService;
    }

    @GetMapping
    public Map<String, Object> getOverallReadiness() {
        var globalScore = prepTrackerService.calculateGlobalReadiness();
        var companies = prepRepository.findAll();
        return Map.of(
                "global_readiness", globalScore,
                "companies", companies
        );
    }

    @GetMapping("/{company}")
    public PrepReadiness getCompanyReadiness(@PathVariable String company) {
        return prepRepository.findByCompanyName(company)
                .orElseThrow(() -> new RuntimeException("No readiness data for " + company));
    }

    @PostMapping("/{company}")
    public PrepReadiness updateCompanyReadiness(
            @PathVariable String company,
            @RequestBody Map<String, Double> scores) {
        return prepTrackerService.updateCompanyReadiness(
                company,
                new java.math.BigDecimal(scores.get("dsa")),
                new java.math.BigDecimal(scores.get("system_design")),
                new java.math.BigDecimal(scores.get("behavioral"))
        );
    }
}
