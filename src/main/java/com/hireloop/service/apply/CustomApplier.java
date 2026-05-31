package com.hireloop.service.apply;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.hireloop.model.Job;
import com.hireloop.model.ResumeMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;

@Component
public class CustomApplier implements AtsApplier {
    private static final Logger log = LoggerFactory.getLogger(CustomApplier.class);

    @Override
    public boolean supports(String atsType) {
        // Handles "custom" and acts as fallback for unknown ATS types
        return "custom".equalsIgnoreCase(atsType) || atsType == null;
    }

    @Override
    public ApplyResult apply(Job job, ResumeMaster resume, ApplyConfig config) {
        String jobId = String.valueOf(job.getId());
        log.info("Custom apply: job={} company={} url={}", jobId, job.getCompanyName(), job.getJdUrl());

        if (job.getJdUrl() == null || job.getJdUrl().isBlank()) {
            return ApplyResult.failure(jobId, job.getCompanyName(), "No job URL available for custom apply");
        }

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(config.isHeadless())
            );
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.navigate(job.getJdUrl());
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Best-effort: fill any visible text inputs with common field patterns
            fillIfPresent(page, "input[name*='first_name'], input[id*='first_name']", config.getFirstName());
            fillIfPresent(page, "input[name*='last_name'], input[id*='last_name']", config.getLastName());
            fillIfPresent(page, "input[name*='name']:not([name*='company']):not([name*='last']):not([name*='first'])", config.getUserName());
            fillIfPresent(page, "input[type='email'], input[name*='email']", config.getUserEmail());
            if (config.getUserPhone() != null && !config.getUserPhone().isBlank()) {
                fillIfPresent(page, "input[type='tel'], input[name*='phone']", config.getUserPhone());
            }

            // Upload resume if file input present
            Locator fileInput = page.locator("input[type='file']").first();
            if (fileInput.count() > 0 && config.getResumePath() != null) {
                try {
                    fileInput.setInputFiles(Paths.get(config.getResumePath()));
                    page.waitForTimeout(1500);
                } catch (Exception e) {
                    log.warn("Could not upload resume file: {}", e.getMessage());
                }
            }

            log.warn("Custom apply for {} partially filled — manual submission required", job.getCompanyName());
            return ApplyResult.failure(jobId, job.getCompanyName(),
                "Custom ATS for " + job.getCompanyName() + " requires manual submission. Form partially filled. URL: " + job.getJdUrl());

        } catch (Exception e) {
            log.error("Custom apply failed for job {}", jobId, e);
            return ApplyResult.failure(jobId, job.getCompanyName(), "Automation error: " + e.getMessage());
        }
    }

    private void fillIfPresent(Page page, String selector, String value) {
        if (value == null || value.isBlank()) return;
        try {
            Locator field = page.locator(selector).first();
            if (field.count() > 0 && field.isVisible()) {
                field.fill(value);
            }
        } catch (Exception ignored) {
        }
    }
}
