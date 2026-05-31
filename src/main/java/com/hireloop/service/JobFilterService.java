package com.hireloop.service;

import com.hireloop.model.Job;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class JobFilterService {
    private static final int MAX_JOB_AGE_DAYS = 30;
    private static final double MIN_FIT_SCORE = 70.0;

    public boolean passesFilters(Job job, List<String> targetLevels, List<String> targetLocations, List<String> excludeKeywords) {
        // Check job age
        if (job.getCreatedAt() != null) {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(MAX_JOB_AGE_DAYS);
            if (job.getCreatedAt().isBefore(thirtyDaysAgo)) {
                return false;
            }
        }

        // Check fit score
        if (job.getFitScore() != null && job.getFitScore().doubleValue() < MIN_FIT_SCORE) {
            return false;
        }

        // Check title for excluded keywords
        String jobTitle = job.getTitle().toLowerCase();
        for (String keyword : excludeKeywords) {
            if (jobTitle.contains(keyword.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    public List<String> getDefaultTargetLevels() {
        return Arrays.asList("Senior", "Staff", "Lead");
    }

    public List<String> getDefaultTargetLocations() {
        return Arrays.asList("Remote", "San Francisco", "New York");
    }

    public List<String> getDefaultExcludeKeywords() {
        return Arrays.asList("Intern", "Junior", "Contract");
    }
}
