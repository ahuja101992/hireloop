package com.hireloop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireloop.model.CompanyIntel;
import com.hireloop.provider.LlmProvider;
import com.hireloop.provider.LlmProviderFactory;
import com.hireloop.repository.CompanyIntelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class BriefService {
    private static final Logger log = LoggerFactory.getLogger(BriefService.class);

    private final CompanyIntelRepository companyIntelRepository;
    private final LlmProviderFactory llmProviderFactory;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BriefService(
            CompanyIntelRepository companyIntelRepository,
            LlmProviderFactory llmProviderFactory,
            NotificationService notificationService) {
        this.companyIntelRepository = companyIntelRepository;
        this.llmProviderFactory = llmProviderFactory;
        this.notificationService = notificationService;
    }

    public JsonNode generateBrief(String companyName) {
        // Get the company's intel data
        Optional<CompanyIntel> intel = companyIntelRepository.findFirstByCompanyName(companyName);
        if (intel.isEmpty()) {
            throw new RuntimeException("No intel found for company: " + companyName);
        }

        CompanyIntel intelData = intel.get();
        String intelJson = intelData.getInterviewRounds();

        // Use LLM to generate the brief
        LlmProvider provider = llmProviderFactory.getProvider();
        JsonNode brief = provider.generateBrief(companyName, intelJson);

        log.info("Generated brief for company: {}", companyName);
        return brief;
    }

    public void generateAndEmailBrief(String companyName, String recipientEmail) {
        try {
            JsonNode brief = generateBrief(companyName);
            String briefEmail = formatBriefEmail(companyName, brief);
            notificationService.notifyReadinessReport(companyName, briefEmail, recipientEmail);
            log.info("Emailed brief for company: {} to {}", companyName, recipientEmail);
        } catch (Exception e) {
            log.error("Error generating and emailing brief for: {}", companyName, e);
        }
    }

    private String formatBriefEmail(String companyName, JsonNode brief) {
        StringBuilder email = new StringBuilder();

        email.append("=== Company Interview Brief: ").append(companyName).append(" ===\n\n");

        // Rounds
        if (brief.has("rounds")) {
            email.append("INTERVIEW ROUNDS:\n");
            JsonNode rounds = brief.get("rounds");
            if (rounds.isArray()) {
                int i = 1;
                for (JsonNode round : rounds) {
                    email.append(String.format("  Round %d: %s\n", i++, round.asText()));
                }
            }
            email.append("\n");
        }

        // Interview tips
        if (brief.has("interview_tips")) {
            email.append("INTERVIEW TIPS:\n");
            JsonNode tips = brief.get("interview_tips");
            if (tips.isArray()) {
                for (JsonNode tip : tips) {
                    email.append("  - ").append(tip.asText()).append("\n");
                }
            }
            email.append("\n");
        }

        // Suggested LC problems
        if (brief.has("suggested_lc_problems")) {
            email.append("SUGGESTED LEETCODE PROBLEMS:\n");
            JsonNode problems = brief.get("suggested_lc_problems");
            if (problems.isArray()) {
                for (JsonNode problem : problems) {
                    email.append("  - ").append(problem.asText()).append("\n");
                }
            }
            email.append("\n");
        }

        // System design cases
        if (brief.has("system_design_cases")) {
            email.append("SYSTEM DESIGN CASES TO STUDY:\n");
            JsonNode cases = brief.get("system_design_cases");
            if (cases.isArray()) {
                for (JsonNode sdCase : cases) {
                    email.append("  - ").append(sdCase.asText()).append("\n");
                }
            }
            email.append("\n");
        }

        // Behavioral focus
        if (brief.has("behavioral_focus")) {
            email.append("BEHAVIORAL FOCUS AREAS:\n");
            JsonNode behavioral = brief.get("behavioral_focus");
            if (behavioral.isArray()) {
                for (JsonNode focus : behavioral) {
                    email.append("  - ").append(focus.asText()).append("\n");
                }
            }
            email.append("\n");
        }

        return email.toString();
    }
}
