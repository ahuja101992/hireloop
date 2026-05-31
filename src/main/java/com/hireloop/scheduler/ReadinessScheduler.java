package com.hireloop.scheduler;

import com.hireloop.service.ReadinessReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReadinessScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReadinessScheduler.class);

    private final ReadinessReportService readinessReportService;

    @Value("${mail.user:}")
    private String userEmail;

    public ReadinessScheduler(ReadinessReportService readinessReportService) {
        this.readinessReportService = readinessReportService;
    }

    @Scheduled(cron = "0 9 * * MON")
    public void sendWeeklyReadinessReport() {
        log.info("Generating weekly readiness report...");
        try {
            if (!userEmail.isEmpty()) {
                readinessReportService.sendReport(userEmail);
                log.info("Weekly readiness report sent");
            } else {
                log.warn("User email not configured, printing report to console");
                System.out.println(readinessReportService.generateReport());
            }
        } catch (Exception e) {
            log.error("Error generating readiness report", e);
        }
    }
}
