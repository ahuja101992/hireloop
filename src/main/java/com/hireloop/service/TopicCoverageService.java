package com.hireloop.service;

import com.hireloop.model.TopicCoverage;
import com.hireloop.model.TopicUniverse;
import com.hireloop.repository.TopicCoverageRepository;
import com.hireloop.repository.TopicUniverseRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TopicCoverageService {
    private final TopicCoverageRepository topicCoverageRepository;
    private final TopicUniverseRepository topicUniverseRepository;

    public TopicCoverageService(
            TopicCoverageRepository topicCoverageRepository,
            TopicUniverseRepository topicUniverseRepository) {
        this.topicCoverageRepository = topicCoverageRepository;
        this.topicUniverseRepository = topicUniverseRepository;
    }

    public TopicCoverage updateTopic(Long topicId, String status, String notes) {
        Optional<TopicCoverage> existing = topicCoverageRepository.findByTopicId(Math.toIntExact(topicId));
        TopicCoverage coverage = existing.orElseGet(TopicCoverage::new);

        coverage.setTopic(topicUniverseRepository.findById(Math.toIntExact(topicId))
                .orElseThrow(() -> new RuntimeException("Topic not found: " + topicId)));
        coverage.setStatus(status);
        coverage.setNotes(notes);
        coverage.setUpdatedAt(LocalDateTime.now());

        return topicCoverageRepository.save(coverage);
    }

    public TopicCoverage markCovered(Long topicId) {
        return updateTopic(topicId, "COVERED", null);
    }

    public TopicCoverage markWeak(Long topicId) {
        return updateTopic(topicId, "WEAK", null);
    }

    public TopicCoverage markInProgress(Long topicId) {
        return updateTopic(topicId, "IN_PROGRESS", null);
    }

    public TopicCoverage markNotStarted(Long topicId) {
        return updateTopic(topicId, "NOT_STARTED", null);
    }

    public List<TopicCoverage> getAllCoverage() {
        return topicCoverageRepository.findAll();
    }

    public TopicCoverage getCoverageByTopicId(Long topicId) {
        return topicCoverageRepository.findByTopicId(Math.toIntExact(topicId))
                .orElseThrow(() -> new RuntimeException("Coverage not found for topic: " + topicId));
    }
}
