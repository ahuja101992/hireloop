package com.hireloop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireloop.config.AppConfig;
import com.hireloop.model.Job;
import com.hireloop.repository.JobRepository;
import com.hireloop.repository.PrepReadinessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobPollerService {
    private static final Logger log = LoggerFactory.getLogger(JobPollerService.class);
    private final JobRepository jobRepository;
    private final PrepReadinessRepository prepReadinessRepository;
    private final JobFilterService jobFilterService;
    private final AppConfig.TargetsConfig targetsConfig;
    private final AppConfig.FiltersConfig filtersConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobPollerService(
            JobRepository jobRepository,
            PrepReadinessRepository prepReadinessRepository,
            JobFilterService jobFilterService,
            AppConfig.TargetsConfig targetsConfig,
            AppConfig.FiltersConfig filtersConfig,
            RestTemplate restTemplate) {
        this.jobRepository = jobRepository;
        this.prepReadinessRepository = prepReadinessRepository;
        this.jobFilterService = jobFilterService;
        this.targetsConfig = targetsConfig;
        this.filtersConfig = filtersConfig;
        this.restTemplate = restTemplate;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void pollAllJobs() {
        log.info("Starting job polling cycle");
        int totalDiscovered = 0;
        int totalFiltered = 0;
        int totalSaved = 0;

        for (AppConfig.TargetsConfig.Company company : targetsConfig.getCompanies()) {
            try {
                int discovered = pollJobsForCompany(company);
                totalDiscovered += discovered;
                totalSaved += discovered;
            } catch (Exception e) {
                log.error("Error polling jobs for {}: {}", company.getName(), e.getMessage(), e);
            }
        }

        log.info("Job polling cycle complete: Discovered {} jobs, saved {} new jobs",
                totalDiscovered, totalSaved);
    }

    public int pollJobsForCompany(AppConfig.TargetsConfig.Company company) {
        log.info("Polling jobs for company: {}", company.getName());

        // Check if company meets readiness threshold
        var readiness = prepReadinessRepository.findByCompanyName(company.getName());
        if (readiness.isPresent()) {
            Double score = readiness.get().getOverallScore() != null ?
                    readiness.get().getOverallScore().doubleValue() : 0.0;
            int threshold = company.getApplyReadinessThreshold() != null ?
                    company.getApplyReadinessThreshold() : filtersConfig.getApplyReadinessThresholdDefault();

            if (score < threshold) {
                log.info("Skipping {} - readiness {}/{}", company.getName(), score, threshold);
                return 0;
            }
        }

        // Fetch jobs from ATS API
        List<Job> discoveredJobs = fetchJobsFromATS(company);
        if (discoveredJobs == null) {
            discoveredJobs = new ArrayList<>();
        }

        log.info("Discovered {} jobs from {}", discoveredJobs.size(), company.getName());

        // Filter jobs
        List<Job> filteredJobs = new ArrayList<>();
        for (Job job : discoveredJobs) {
            if (jobFilterService.passesAllFilters(job, filtersConfig)) {
                filteredJobs.add(job);
            }
        }

        log.info("Filtered to {} jobs after applying filters", filteredJobs.size());

        // Save new jobs
        int savedCount = 0;
        for (Job job : filteredJobs) {
            // Check if job already exists
            List<Job> existing = jobRepository.findByCompanyNameAndTitle(job.getCompanyName(), job.getTitle());
            if (existing.isEmpty()) {
                job.setStatus("NEW");
                job.setDiscoveredAt(LocalDateTime.now());
                jobRepository.save(job);
                savedCount++;
            }
        }

        log.info("Saved {} new jobs to database", savedCount);
        return savedCount;
    }

    private List<Job> fetchJobsFromATS(AppConfig.TargetsConfig.Company company) {
        String atsType = company.getAts().toLowerCase();

        switch (atsType) {
            case "greenhouse":
                return fetchFromGreenhouse(company);
            case "lever":
                return fetchFromLever(company);
            case "workday":
                log.warn("Workday scraping not yet implemented for {}", company.getName());
                return new ArrayList<>();
            default:
                log.warn("Unknown ATS type: {} for {}", atsType, company.getName());
                return new ArrayList<>();
        }
    }

    private List<Job> fetchFromGreenhouse(AppConfig.TargetsConfig.Company company) {
        try {
            log.debug("Fetching from Greenhouse API: {}", company.getApiUrl());
            String response = restTemplate.getForObject(company.getApiUrl(), String.class);
            JsonNode root = objectMapper.readTree(response);

            List<Job> jobs = new ArrayList<>();
            JsonNode jobsArray = root.get("jobs");

            if (jobsArray != null && jobsArray.isArray()) {
                for (JsonNode jobNode : jobsArray) {
                    Job job = parseGreenhouseJob(jobNode, company.getName());
                    jobs.add(job);
                }
            }

            log.info("Parsed {} jobs from Greenhouse for {}", jobs.size(), company.getName());
            return jobs;
        } catch (RestClientException e) {
            log.error("Error fetching from Greenhouse API: {}", e.getMessage(), e);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error parsing Greenhouse response: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<Job> fetchFromLever(AppConfig.TargetsConfig.Company company) {
        try {
            log.debug("Fetching from Lever API: {}", company.getApiUrl());
            String response = restTemplate.getForObject(company.getApiUrl(), String.class);
            JsonNode root = objectMapper.readTree(response);

            List<Job> jobs = new ArrayList<>();
            JsonNode postingsArray = root.get("postings");

            if (postingsArray != null && postingsArray.isArray()) {
                for (JsonNode postingNode : postingsArray) {
                    Job job = parseLeverJob(postingNode, company.getName());
                    jobs.add(job);
                }
            }

            log.info("Parsed {} jobs from Lever for {}", jobs.size(), company.getName());
            return jobs;
        } catch (RestClientException e) {
            log.error("Error fetching from Lever API: {}", e.getMessage(), e);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error parsing Lever response: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private Job parseGreenhouseJob(JsonNode jobNode, String companyName) {
        Job job = new Job();
        job.setCompanyName(companyName);
        job.setTitle(jobNode.path("title").asText(""));
        job.setJdUrl(jobNode.path("absolute_url").asText(""));
        job.setAtsType("greenhouse");

        // Extract job description if available
        String jobDescription = jobNode.path("content").asText("");
        job.setJdText(jobDescription);

        // Try to parse posted_at date
        String postedAt = jobNode.path("created_at").asText();
        if (!postedAt.isEmpty()) {
            try {
                job.setPostedAt(LocalDateTime.parse(postedAt.replace("Z", "+00:00")));
            } catch (Exception e) {
                log.debug("Could not parse date: {}", postedAt);
            }
        }

        return job;
    }

    private Job parseLeverJob(JsonNode postingNode, String companyName) {
        Job job = new Job();
        job.setCompanyName(companyName);
        job.setTitle(postingNode.path("title").asText(""));
        job.setJdUrl(postingNode.path("urls").path("show").asText(""));
        job.setAtsType("lever");

        // Extract job description if available
        String jobDescription = postingNode.path("description").asText("");
        job.setJdText(jobDescription);

        // Try to parse createdAt date
        String createdAt = postingNode.path("createdAt").asText();
        if (!createdAt.isEmpty()) {
            try {
                job.setPostedAt(LocalDateTime.parse(createdAt.replace("Z", "+00:00")));
            } catch (Exception e) {
                log.debug("Could not parse date: {}", createdAt);
            }
        }

        return job;
    }

    public List<Job> getPendingJobs() {
        return jobRepository.findByStatus("PENDING");
    }
}
