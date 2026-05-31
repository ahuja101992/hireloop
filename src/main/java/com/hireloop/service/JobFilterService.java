package com.hireloop.service;

import com.hireloop.config.AppConfig;
import com.hireloop.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class JobFilterService {
    private static final Logger log = LoggerFactory.getLogger(JobFilterService.class);

    public boolean passesAllFilters(Job job, AppConfig.FiltersConfig filtersConfig) {
        // Check job age
        if (!passesAgeFilter(job, filtersConfig)) {
            log.debug("Job {} filtered out: too old", job.getId());
            return false;
        }

        // Check title for target levels
        if (!passesTitleLevelFilter(job, filtersConfig)) {
            log.debug("Job {} filtered out: title doesn't match target levels", job.getId());
            return false;
        }

        // Check for exclude keywords in title and JD
        if (!passesExcludeKeywordsFilter(job, filtersConfig)) {
            log.debug("Job {} filtered out: contains exclude keywords", job.getId());
            return false;
        }

        // Check for require keywords in JD
        if (!passesRequireKeywordsFilter(job, filtersConfig)) {
            log.debug("Job {} filtered out: missing require keywords", job.getId());
            return false;
        }

        return true;
    }

    private boolean passesAgeFilter(Job job, AppConfig.FiltersConfig filtersConfig) {
        LocalDateTime postedAt = job.getPostedAt();
        if (postedAt == null) {
            // If no posted date, use created date as fallback
            postedAt = job.getCreatedAt();
        }

        if (postedAt == null) {
            // No date information - let it pass
            return true;
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(filtersConfig.getMaxAgeDays());
        return postedAt.isAfter(cutoffDate) || postedAt.isEqual(cutoffDate);
    }

    private boolean passesTitleLevelFilter(Job job, AppConfig.FiltersConfig filtersConfig) {
        String title = job.getTitle().toLowerCase();
        List<String> targetLevels = filtersConfig.getTargetLevels();

        // If no target levels specified, accept all
        if (targetLevels == null || targetLevels.isEmpty()) {
            return true;
        }

        // Check if any target level appears in title
        for (String level : targetLevels) {
            if (title.contains(level.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private boolean passesExcludeKeywordsFilter(Job job, AppConfig.FiltersConfig filtersConfig) {
        List<String> excludeKeywords = filtersConfig.getExcludeKeywords();
        if (excludeKeywords == null || excludeKeywords.isEmpty()) {
            return true;
        }

        String title = job.getTitle().toLowerCase();
        String jd = job.getJdText() != null ? job.getJdText().toLowerCase() : "";

        for (String keyword : excludeKeywords) {
            if (title.contains(keyword.toLowerCase()) || jd.contains(keyword.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    private boolean passesRequireKeywordsFilter(Job job, AppConfig.FiltersConfig filtersConfig) {
        List<String> requireKeywords = filtersConfig.getRequireKeywords();
        if (requireKeywords == null || requireKeywords.isEmpty()) {
            return true;
        }

        String jd = job.getJdText() != null ? job.getJdText().toLowerCase() : "";

        for (String keyword : requireKeywords) {
            if (!jd.contains(keyword.toLowerCase())) {
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
