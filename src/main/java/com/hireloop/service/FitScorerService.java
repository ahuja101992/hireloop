package com.hireloop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireloop.model.Job;
import com.hireloop.provider.LlmProvider;
import com.hireloop.provider.LlmProviderFactory;
import com.hireloop.repository.JobRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class FitScorerService {
    private final JobRepository jobRepository;
    private final LlmProviderFactory llmProviderFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FitScorerService(
            JobRepository jobRepository,
            LlmProviderFactory llmProviderFactory) {
        this.jobRepository = jobRepository;
        this.llmProviderFactory = llmProviderFactory;
    }

    public void scoreJob(Integer jobId, String resume) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        LlmProvider provider = llmProviderFactory.getProvider();
        JsonNode scoreResult = provider.scoreJob(resume, job.getJdText());

        // Extract fit score from response
        BigDecimal fitScore = new BigDecimal(scoreResult.get("fit_score").asDouble());
        job.setFitScore(fitScore);
        job.setStatus("SCORED");
        job.setUpdatedAt(LocalDateTime.now());

        jobRepository.save(job);
    }

    public void scoreAllJobs(String resume) {
        var jobs = jobRepository.findByStatus("PENDING");
        for (Job job : jobs) {
            try {
                scoreJob(job.getId(), resume);
            } catch (Exception e) {
                System.err.println("Error scoring job " + job.getId() + ": " + e.getMessage());
            }
        }
    }
}
