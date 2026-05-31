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
            BriefService briefService) {
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

                // Find topic by name and update
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
}
