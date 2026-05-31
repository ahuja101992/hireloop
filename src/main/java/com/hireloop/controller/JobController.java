package com.hireloop.controller;

import com.hireloop.model.Job;
import com.hireloop.repository.JobRepository;
import com.hireloop.service.FitScorerService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:3000")
public class JobController {
    private final JobRepository jobRepository;
    private final FitScorerService fitScorerService;

    public JobController(
            JobRepository jobRepository,
            FitScorerService fitScorerService) {
        this.jobRepository = jobRepository;
        this.fitScorerService = fitScorerService;
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
    public Job confirmApply(@PathVariable Integer id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        job.setConfirmed(true);
        return jobRepository.save(job);
    }

    @PostMapping("/{id}/skip")
    public Job skipJob(@PathVariable Integer id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        job.setStatus("SKIPPED");
        return jobRepository.save(job);
    }
}
