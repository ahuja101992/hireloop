package com.hireloop.controller;

import com.hireloop.model.Job;
import com.hireloop.repository.JobRepository;
import com.hireloop.service.ApplyEngineService;
import com.hireloop.service.FitScorerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:3000")
public class JobController {
    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    private final JobRepository jobRepository;
    private final FitScorerService fitScorerService;
    private final ApplyEngineService applyEngineService;

    public JobController(
            JobRepository jobRepository,
            FitScorerService fitScorerService,
            ApplyEngineService applyEngineService) {
        this.jobRepository = jobRepository;
        this.fitScorerService = fitScorerService;
        this.applyEngineService = applyEngineService;
    }

    @GetMapping
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/{id}")
    public Job getJob(@PathVariable Integer id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
    }

    @PostMapping
    public Job createJob(@RequestBody Job job) {
        return jobRepository.save(job);
    }

    @PostMapping("/{id}/score")
    public void scoreJob(
            @PathVariable Integer id,
            @RequestBody ScoreJobRequest request) {
        fitScorerService.scoreJob(id, request.getResume());
    }

    @PostMapping("/{id}/confirm-apply")
    public Map<String, Object> confirmApply(@PathVariable Integer id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        job.setConfirmed(true);
        jobRepository.save(job);

        // If auto-apply is on, trigger apply asynchronously
        if (applyEngineService.isAutoApplyEnabled()) {
            log.info("Auto-apply enabled — triggering apply for job {}", id);
            new Thread(() -> {
                try {
                    applyEngineService.applyToJob(id);
                } catch (Exception e) {
                    log.error("Auto-apply failed for job {}: {}", id, e.getMessage());
                }
            }).start();
            return Map.of(
                "confirmed", true,
                "autoApplyTriggered", true,
                "message", "Job confirmed and auto-apply triggered"
            );
        }

        return Map.of(
            "confirmed", true,
            "autoApplyTriggered", false,
            "message", "Job confirmed. Use POST /api/apply/" + id + " to apply manually."
        );
    }

    @PostMapping("/{id}/skip")
    public Job skipJob(@PathVariable Integer id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        job.setStatus("SKIPPED");
        return jobRepository.save(job);
    }
}
