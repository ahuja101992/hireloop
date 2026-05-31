package com.hireloop.controller;

import com.hireloop.model.TopicUniverse;
import com.hireloop.repository.TopicUniverseRepository;
import com.hireloop.service.PrepTrackerService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "http://localhost:3000")
public class TopicController {
    private final TopicUniverseRepository topicRepository;
    private final PrepTrackerService prepTrackerService;

    public TopicController(
            TopicUniverseRepository topicRepository,
            PrepTrackerService prepTrackerService) {
        this.topicRepository = topicRepository;
        this.prepTrackerService = prepTrackerService;
    }

    @GetMapping
    public List<TopicUniverse> getAllTopics() {
        return topicRepository.findAll();
    }

    @PostMapping
    public TopicUniverse createTopic(@RequestBody TopicUniverse topic) {
        return topicRepository.save(topic);
    }

    @PostMapping("/{id}/update")
    public void updateTopicCoverage(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        String notes = request.get("notes");
        prepTrackerService.updateTopicCoverage(id, status, notes);
    }
}
