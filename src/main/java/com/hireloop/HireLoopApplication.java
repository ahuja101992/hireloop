package com.hireloop;

import com.hireloop.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HireLoopApplication {
    private static final Logger log = LoggerFactory.getLogger(HireLoopApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(HireLoopApplication.class, args);
    }

    @Bean
    public ApplicationRunner cliRunner(
            ResumeParserService resumeParserService,
            IntelScrapeService intelScrapeService,
            TopicCoverageService topicCoverageService,
            ReadinessReportService readinessReportService,
            BriefService briefService,
            ApplyEngineService applyEngineService) {
        return args -> {
            if (args.containsOption("load-resume")) {
                handleLoadResume(resumeParserService);
            }

            if (args.containsOption("scrape-intel")) {
                handleScrapeIntel(intelScrapeService);
            }

            if (args.containsOption("update-topic")) {
                handleUpdateTopic(args, topicCoverageService);
            }

            if (args.containsOption("show-readiness")) {
                handleShowReadiness(readinessReportService);
            }

            if (args.containsOption("brief")) {
                handleBrief(args, briefService);
            }

            if (args.containsOption("apply")) {
                handleApply(args, applyEngineService);
            }

            if (args.containsOption("apply-all")) {
                handleApplyAll(applyEngineService);
            }
        };
    }

    private void handleLoadResume(ResumeParserService resumeParserService) {
        try {
            log.info("Loading resume from resume.docx");
            resumeParserService.parseAndStore("resume/resume.docx");
            System.out.println("Resume loaded and stored successfully");
        } catch (Exception e) {
            log.error("Error loading resume", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void handleScrapeIntel(IntelScrapeService intelScrapeService) {
        try {
            log.info("Starting manual intel scrape");
            intelScrapeService.scrapeAllIntel();
            System.out.println("Intel scrape completed successfully");
        } catch (Exception e) {
            log.error("Error scraping intel", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void handleUpdateTopic(ApplicationArguments args, TopicCoverageService topicCoverageService) {
        try {
            var topicValues = args.getOptionValues("update-topic");
            if (topicValues != null && topicValues.size() >= 3) {
                String topic = topicValues.get(0);
                String status = topicValues.get(1);
                String notes = topicValues.get(2);
                log.info("Updating topic: {} to status: {}", topic, status);
                System.out.println(String.format("Updated topic '%s' to %s", topic, status));
            } else {
                System.err.println("Usage: --update-topic \"[topic]\" [STATUS] \"[notes]\"");
            }
        } catch (Exception e) {
            log.error("Error updating topic", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void handleShowReadiness(ReadinessReportService readinessReportService) {
        try {
            log.info("Generating readiness report");
            String report = readinessReportService.generateReport();
            System.out.println(report);
        } catch (Exception e) {
            log.error("Error generating readiness report", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void handleBrief(ApplicationArguments args, BriefService briefService) {
        try {
            var briefValues = args.getOptionValues("brief");
            if (briefValues != null && !briefValues.isEmpty()) {
                String companyName = briefValues.get(0);
                log.info("Generating brief for company: {}", companyName);
                var brief = briefService.generateBrief(companyName);
                System.out.println("Brief for " + companyName + ":");
                System.out.println(brief.toPrettyString());
            } else {
                System.err.println("Usage: --brief [company]");
            }
        } catch (Exception e) {
            log.error("Error generating brief", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void handleApply(ApplicationArguments args, ApplyEngineService applyEngineService) {
        try {
            var applyValues = args.getOptionValues("apply");
            if (applyValues != null && !applyValues.isEmpty()) {
                Integer jobId = Integer.parseInt(applyValues.get(0));
                log.info("Applying to job: {}", jobId);
                var result = applyEngineService.applyToJob(jobId);
                System.out.println(result.isSuccess()
                    ? "✓ Applied to " + result.getCompanyName() + " (appId=" + result.getApplicationId() + ")"
                    : "✗ Apply failed: " + result.getErrorMessage());
            } else {
                System.err.println("Usage: --apply [jobId]");
            }
        } catch (Exception e) {
            log.error("Error applying to job", e);
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void handleApplyAll(ApplyEngineService applyEngineService) {
        try {
            log.info("Starting batch apply for all confirmed jobs");
            var results = applyEngineService.applyBatch();
            long succeeded = results.stream().filter(r -> r.isSuccess()).count();
            System.out.println(String.format("Batch apply complete: %d/%d succeeded", succeeded, results.size()));
            results.forEach(r -> System.out.println(
                (r.isSuccess() ? "  ✓ " : "  ✗ ") + r.getCompanyName() + ": " +
                (r.isSuccess() ? "Applied" : r.getErrorMessage())
            ));
        } catch (Exception e) {
            log.error("Error in batch apply", e);
            System.err.println("Error: " + e.getMessage());
        }
    }
}
