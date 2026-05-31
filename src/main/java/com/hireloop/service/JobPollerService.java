package com.hireloop.service;

import com.hireloop.model.Job;
import com.hireloop.repository.JobRepository;
import com.hireloop.repository.PrepReadinessRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobPollerService {
    private final JobRepository jobRepository;
    private final PrepReadinessRepository prepReadinessRepository;

    public JobPollerService(
            JobRepository jobRepository,
            PrepReadinessRepository prepReadinessRepository) {
        this.jobRepository = jobRepository;
        this.prepReadinessRepository = prepReadinessRepository;
    }

    public void pollJobsForCompanies(List<String> companies) {
        for (String company : companies) {
            pollJobsForCompany(company);
        }
    }

    public void pollJobsForCompany(String companyName) {
        try {
            // Check if company meets readiness threshold (70%)
            var readiness = prepReadinessRepository.findByCompanyName(companyName);
            if (readiness.isPresent() && readiness.get().getOverallScore().doubleValue() < 70.0) {
                System.out.println("Skipping polling for " + companyName + " - insufficient readiness");
                return;
            }

            // Mock job polling - in production this would call ATS APIs
            List<Job> newJobs = new ArrayList<>();
            // Placeholder: would fetch from LinkedIn, company ATS, etc.

            for (Job job : newJobs) {
                jobRepository.save(job);
            }
        } catch (Exception e) {
            System.err.println("Error polling jobs for " + companyName + ": " + e.getMessage());
        }
    }

    public List<Job> getPendingJobs() {
        return jobRepository.findByStatus("PENDING");
    }
}
