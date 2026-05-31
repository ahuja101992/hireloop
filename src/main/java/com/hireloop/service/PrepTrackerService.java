package com.hireloop.service;

import com.hireloop.model.PrepReadiness;
import com.hireloop.model.TopicCoverage;
import com.hireloop.repository.PrepReadinessRepository;
import com.hireloop.repository.TopicCoverageRepository;
import com.hireloop.repository.TopicUniverseRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PrepTrackerService {
    private final PrepReadinessRepository prepReadinessRepository;
    private final TopicCoverageRepository topicCoverageRepository;
    private final TopicUniverseRepository topicUniverseRepository;

    public PrepTrackerService(
            PrepReadinessRepository prepReadinessRepository,
            TopicCoverageRepository topicCoverageRepository,
            TopicUniverseRepository topicUniverseRepository) {
        this.prepReadinessRepository = prepReadinessRepository;
        this.topicCoverageRepository = topicCoverageRepository;
        this.topicUniverseRepository = topicUniverseRepository;
    }

    public void updateTopicCoverage(Integer topicId, String status, String notes) {
        Optional<TopicCoverage> coverage = topicCoverageRepository.findByTopicId(topicId);
        TopicCoverage tc = coverage.orElseGet(TopicCoverage::new);
        tc.setTopic(topicUniverseRepository.findById(topicId).orElse(null));
        tc.setStatus(status);
        tc.setNotes(notes);
        tc.setUpdatedAt(LocalDateTime.now());
        topicCoverageRepository.save(tc);
    }

    public BigDecimal calculateGlobalReadiness() {
        List<TopicCoverage> coverages = topicCoverageRepository.findAll();
        long completedCount = coverages.stream()
                .filter(tc -> "COMPLETED".equals(tc.getStatus()))
                .count();
        long totalCount = coverages.size();

        if (totalCount == 0) return BigDecimal.ZERO;
        return new BigDecimal(completedCount * 100).divide(new BigDecimal(totalCount), 2, BigDecimal.ROUND_HALF_UP);
    }

    public PrepReadiness updateCompanyReadiness(String companyName, BigDecimal dsa, BigDecimal systemDesign, BigDecimal behavioral) {
        Optional<PrepReadiness> existing = prepReadinessRepository.findByCompanyName(companyName);
        PrepReadiness readiness = existing.orElseGet(PrepReadiness::new);

        readiness.setCompanyName(companyName);
        readiness.setDsaScore(dsa);
        readiness.setSystemDesignScore(systemDesign);
        readiness.setBehavioralScore(behavioral);

        // Calculate overall as weighted average: DSA 40%, SystemDesign 40%, Behavioral 20%
        BigDecimal overall = dsa.multiply(new BigDecimal("0.4"))
                .add(systemDesign.multiply(new BigDecimal("0.4")))
                .add(behavioral.multiply(new BigDecimal("0.2")));
        readiness.setOverallScore(overall);
        readiness.setLastUpdated(LocalDateTime.now());

        return prepReadinessRepository.save(readiness);
    }
}
