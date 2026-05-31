package com.hireloop.service;

import com.hireloop.model.PrepReadiness;
import com.hireloop.model.TopicCoverage;
import com.hireloop.model.TopicUniverse;
import com.hireloop.repository.PrepReadinessRepository;
import com.hireloop.repository.TopicCoverageRepository;
import com.hireloop.repository.TopicUniverseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReadinessReportService {
    private static final Logger log = LoggerFactory.getLogger(ReadinessReportService.class);

    private final PrepReadinessRepository prepReadinessRepository;
    private final TopicCoverageRepository topicCoverageRepository;
    private final TopicUniverseRepository topicUniverseRepository;
    private final NotificationService notificationService;

    public ReadinessReportService(
            PrepReadinessRepository prepReadinessRepository,
            TopicCoverageRepository topicCoverageRepository,
            TopicUniverseRepository topicUniverseRepository,
            NotificationService notificationService) {
        this.prepReadinessRepository = prepReadinessRepository;
        this.topicCoverageRepository = topicCoverageRepository;
        this.topicUniverseRepository = topicUniverseRepository;
        this.notificationService = notificationService;
    }

    public String generateReport() {
        StringBuilder report = new StringBuilder();

        // Get global readiness
        List<TopicCoverage> allCoverages = topicCoverageRepository.findAll();
        Map<String, List<TopicCoverage>> byCategory = categorizeTopics(allCoverages);

        // Header
        report.append("=== HireLoop Readiness Report ===\n");
        report.append("Generated: ").append(new Date()).append("\n\n");

        // Global scores by category
        report.append("GLOBAL READINESS BY CATEGORY:\n");
        report.append("  DSA:           ").append(calculateCategoryScore("DSA", byCategory)).append("%\n");
        report.append("  System Design: ").append(calculateCategoryScore("SYSTEM_DESIGN", byCategory)).append("%\n");
        report.append("  Behavioral:    ").append(calculateCategoryScore("BEHAVIORAL", byCategory)).append("%\n\n");

        // Per-company readiness
        report.append("PER COMPANY READINESS:\n");
        List<PrepReadiness> companies = prepReadinessRepository.findAll();
        for (PrepReadiness company : companies) {
            if (company.getCompanyName() != null) {
                report.append(String.format("  %-15s : %3d/100 ",
                    company.getCompanyName(), company.getOverallScore().intValue()));
                report.append(company.getOverallScore().compareTo(BigDecimal.valueOf(80)) >= 0 ? "✅ READY\n" : "❌ NOT READY\n");
            }
        }

        report.append("\n");

        // Top topics to study
        report.append("TOP TOPICS TO STUDY (by ROI):\n");
        List<TopicInfo> topicsToStudy = findTopicsToStudy();
        int count = 0;
        for (TopicInfo topic : topicsToStudy) {
            if (count >= 3) break;
            report.append(String.format("  %d. %s (unlocks %d companies)\n",
                ++count, topic.name, topic.unlockCount));
        }

        report.append("\n");
        return report.toString();
    }

    public void sendReport(String recipientEmail) {
        String report = generateReport();
        System.out.println(report); // Print to console
        notificationService.notifyReadinessReport("Global", report, recipientEmail);
    }

    private Map<String, List<TopicCoverage>> categorizeTopics(List<TopicCoverage> coverages) {
        Map<String, List<TopicCoverage>> result = new HashMap<>();
        result.put("DSA", new ArrayList<>());
        result.put("SYSTEM_DESIGN", new ArrayList<>());
        result.put("BEHAVIORAL", new ArrayList<>());

        for (TopicCoverage coverage : coverages) {
            TopicUniverse topic = coverage.getTopic();
            if (topic != null) {
                String category = topic.getCategory();
                result.getOrDefault(category, new ArrayList<>()).add(coverage);
            }
        }

        return result;
    }

    private int calculateCategoryScore(String category, Map<String, List<TopicCoverage>> byCategory) {
        List<TopicCoverage> topics = byCategory.getOrDefault(category, new ArrayList<>());
        if (topics.isEmpty()) return 0;

        long covered = topics.stream()
                .filter(t -> "COVERED".equals(t.getStatus()))
                .count();
        long inProgress = topics.stream()
                .filter(t -> "IN_PROGRESS".equals(t.getStatus()))
                .count();

        BigDecimal score = BigDecimal.valueOf(covered).multiply(BigDecimal.ONE)
                .add(BigDecimal.valueOf(inProgress).multiply(new BigDecimal("0.5")))
                .divide(BigDecimal.valueOf(topics.size()), 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return score.intValue();
    }

    private List<TopicInfo> findTopicsToStudy() {
        List<TopicCoverage> allCoverages = topicCoverageRepository.findAll();

        // Find weak/not started topics
        List<TopicInfo> weakTopics = new ArrayList<>();
        for (TopicCoverage coverage : allCoverages) {
            if (!"COVERED".equals(coverage.getStatus())) {
                TopicUniverse topic = coverage.getTopic();
                if (topic != null) {
                    // Count how many companies need this topic
                    int unlockCount = countCompaniesNeedingTopic(topic);
                    weakTopics.add(new TopicInfo(topic.getTopic(), unlockCount));
                }
            }
        }

        // Sort by unlock count (highest ROI first)
        weakTopics.sort((a, b) -> Integer.compare(b.unlockCount, a.unlockCount));
        return weakTopics;
    }

    private int countCompaniesNeedingTopic(TopicUniverse topic) {
        // This would require checking company_topic_frequency table
        // For now, return 0 as a placeholder
        return 1; // Simplified for this phase
    }

    private static class TopicInfo {
        String name;
        int unlockCount;

        TopicInfo(String name, int unlockCount) {
            this.name = name;
            this.unlockCount = unlockCount;
        }
    }
}
