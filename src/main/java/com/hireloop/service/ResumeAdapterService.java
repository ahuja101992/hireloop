package com.hireloop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireloop.model.Job;
import com.hireloop.provider.LlmProvider;
import com.hireloop.provider.LlmProviderFactory;
import com.hireloop.repository.JobRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class ResumeAdapterService {
    private final JobRepository jobRepository;
    private final LlmProviderFactory llmProviderFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeAdapterService(
            JobRepository jobRepository,
            LlmProviderFactory llmProviderFactory) {
        this.jobRepository = jobRepository;
        this.llmProviderFactory = llmProviderFactory;
    }

    public void adaptResumeForJob(Integer jobId, String baseResume) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        LlmProvider provider = llmProviderFactory.getProvider();
        JsonNode adaptedResume = provider.adaptResume(baseResume, job.getJdText());

        // Store tailored resume as JSON string
        job.setTailoredResumeJson(adaptedResume.toString());
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    public String getTailoredResume(Integer jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        return job.getTailoredResumeJson();
    }
}
