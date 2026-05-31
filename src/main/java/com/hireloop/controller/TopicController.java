package com.hireloop.controller;

import com.hireloop.model.TopicCoverage;
import com.hireloop.model.TopicUniverse;
import com.hireloop.repository.TopicCoverageRepository;
import com.hireloop.repository.TopicUniverseRepository;
import com.hireloop.service.TopicCoverageService;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "http://localhost:3000")
public class TopicController {
    private final TopicUniverseRepository topicRepository;
    private final TopicCoverageRepository topicCoverageRepository;
    private final TopicCoverageService topicCoverageService;

    public TopicController(
            TopicUniverseRepository topicRepository,
            TopicCoverageRepository topicCoverageRepository,
            TopicCoverageService topicCoverageService) {
        this.topicRepository = topicRepository;
        this.topicCoverageRepository = topicCoverageRepository;
        this.topicCoverageService = topicCoverageService;
    }

    @GetMapping
    public List<Map<String, Object>> getAllTopics() {
        List<TopicUniverse> topics = topicRepository.findAll();
        return topics.stream().map(topic -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", topic.getId());
            dto.put("category", topic.getCategory());
            dto.put("topic", topic.getTopic());
            dto.put("global_frequency", topic.getGlobalFrequency());

            var coverage = topicCoverageRepository.findByTopicId(Math.toIntExact(topic.getId()));
            dto.put("coverage_status", coverage.map(TopicCoverage::getStatus).orElse("NOT_STARTED"));
            dto.put("notes", coverage.map(TopicCoverage::getNotes).orElse(""));
            dto.put("updated_at", coverage.map(TopicCoverage::getUpdatedAt).orElse(null));

            return dto;
        }).collect(Collectors.toList());
    }

    @PostMapping
    public TopicUniverse createTopic(@RequestBody TopicUniverse topic) {
        return topicRepository.save(topic);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getTopicDetail(@PathVariable Long id) {
        TopicUniverse topic = topicRepository.findById(Math.toIntExact(id))
                .orElseThrow(() -> new RuntimeException("Topic not found: " + id));

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", topic.getId());
        dto.put("category", topic.getCategory());
        dto.put("topic", topic.getTopic());
        dto.put("global_frequency", topic.getGlobalFrequency());
        dto.put("created_at", topic.getCreatedAt());
        dto.put("updated_at", topic.getUpdatedAt());

        var coverage = topicCoverageRepository.findByTopicId(Math.toIntExact(id));
        if (coverage.isPresent()) {
            dto.put("coverage_status", coverage.get().getStatus());
            dto.put("notes", coverage.get().getNotes());
            dto.put("coverage_updated_at", coverage.get().getUpdatedAt());
        } else {
            dto.put("coverage_status", "NOT_STARTED");
            dto.put("notes", "");
            dto.put("coverage_updated_at", null);
        }

        return dto;
    }

    @PostMapping("/{id}/update")
    public TopicCoverage updateTopicCoverage(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.getOrDefault("status", "NOT_STARTED");
        String notes = request.getOrDefault("notes", "");
        return topicCoverageService.updateTopic(id, status, notes);
    }
}
