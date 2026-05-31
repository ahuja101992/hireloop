package com.hireloop.controller;

import com.hireloop.model.CompanyIntel;
import com.hireloop.repository.CompanyIntelRepository;
import com.hireloop.repository.TopicUniverseRepository;
import com.hireloop.service.BriefService;
import com.hireloop.service.IntelScrapeService;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intel")
@CrossOrigin(origins = "http://localhost:3000")
public class IntelController {
    private final IntelScrapeService intelScrapeService;
    private final CompanyIntelRepository companyIntelRepository;
    private final TopicUniverseRepository topicUniverseRepository;
    private final BriefService briefService;

    public IntelController(
            IntelScrapeService intelScrapeService,
            CompanyIntelRepository companyIntelRepository,
            TopicUniverseRepository topicUniverseRepository,
            BriefService briefService) {
        this.intelScrapeService = intelScrapeService;
        this.companyIntelRepository = companyIntelRepository;
        this.topicUniverseRepository = topicUniverseRepository;
        this.briefService = briefService;
    }

    @PostMapping("/scrape-all")
    public Map<String, Object> scrapeAll() {
        intelScrapeService.scrapeAllIntel();
        return Map.of(
            "status", "SCHEDULED",
            "scheduled_for", "immediately"
        );
    }

    @GetMapping("/{company}")
    public Map<String, Object> getCompanyIntel(@PathVariable String company) {
        var intel = companyIntelRepository.findFirstByCompanyName(company)
                .orElseThrow(() -> new RuntimeException("No intel found for company: " + company));

        Map<String, Object> response = new HashMap<>();
        response.put("company", company);
        response.put("rounds", intel.getInterviewRounds());
        response.put("last_scraped_at", intel.getScrapedAt());
        response.put("sources", List.of(intel.getSource()));

        return response;
    }

    @GetMapping("/aggregate")
    public Map<String, Object> getAggregate() {
        var topics = topicUniverseRepository.findAll();
        return Map.of(
            "topic_universe", topics,
            "global_patterns", Map.of()
        );
    }

    @PostMapping("/brief/{company}")
    public Map<String, Object> generateBrief(@PathVariable String company) {
        var brief = briefService.generateBrief(company);
        return Map.of(
            "company", company,
            "brief", brief,
            "generated_at", java.time.LocalDateTime.now()
        );
    }

    @GetMapping("/brief/{company}")
    public Map<String, Object> getBrief(@PathVariable String company) {
        try {
            var brief = briefService.generateBrief(company);
            return Map.of(
                "company", company,
                "brief", brief,
                "generated_at", java.time.LocalDateTime.now()
            );
        } catch (Exception e) {
            return Map.of(
                "error", e.getMessage()
            );
        }
    }
}
