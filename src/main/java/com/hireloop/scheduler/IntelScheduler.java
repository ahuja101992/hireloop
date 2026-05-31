package com.hireloop.scheduler;

import com.hireloop.service.IntelScrapeService;
import com.hireloop.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class IntelScheduler {
    private static final Logger log = LoggerFactory.getLogger(IntelScheduler.class);

    private final IntelScrapeService intelScrapeService;
    private final NotificationService notificationService;

    @Value("${mail.user:}")
    private String userEmail;

    public IntelScheduler(
            IntelScrapeService intelScrapeService,
            NotificationService notificationService) {
        this.intelScrapeService = intelScrapeService;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 0 * * SUN")
    public void scrapeIntelWeekly() {
        log.info("Starting weekly intel scrape...");
        try {
            intelScrapeService.scrapeAllIntel();
            log.info("Weekly intel scrape completed successfully");

            if (!userEmail.isEmpty()) {
                notificationService.notifyReadinessReport(
                    "Intel Refresh",
                    "Intel refresh complete. Topic universe updated.",
                    userEmail
                );
            }
        } catch (Exception e) {
            log.error("Error during weekly intel scrape", e);
        }
    }
}
