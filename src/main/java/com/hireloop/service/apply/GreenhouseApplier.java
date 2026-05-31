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
public class GreenhouseApplier implements AtsApplier {
    private static final Logger log = LoggerFactory.getLogger(GreenhouseApplier.class);

    @Override
    public boolean supports(String atsType) {
        return "greenhouse".equalsIgnoreCase(atsType);
    }

    @Override
    public ApplyResult apply(Job job, ResumeMaster resume, ApplyConfig config) {
        String jobId = String.valueOf(job.getId());
        log.info("Greenhouse apply: job={} company={} url={}", jobId, job.getCompanyName(), job.getJdUrl());

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(config.isHeadless())
            );
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.navigate(job.getJdUrl());
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Click "Apply for this Job" button (Greenhouse standard)
            Locator applyBtn = page.locator("a:has-text('Apply'), button:has-text('Apply')").first();
            if (applyBtn.count() > 0) {
                applyBtn.click();
                page.waitForLoadState(LoadState.NETWORKIDLE);
            }

            // Fill first name
            Locator firstNameField = page.locator("input[id*='first_name'], input[name*='first_name']").first();
            if (firstNameField.count() > 0) {
                firstNameField.fill(config.getFirstName());
            }

            // Fill last name
            Locator lastNameField = page.locator("input[id*='last_name'], input[name*='last_name']").first();
            if (lastNameField.count() > 0) {
                lastNameField.fill(config.getLastName());
            }

            // Fill email
            Locator emailField = page.locator("input[id*='email'], input[name*='email'], input[type='email']").first();
            if (emailField.count() > 0) {
                emailField.fill(config.getUserEmail());
            }

            // Fill phone
            if (config.getUserPhone() != null && !config.getUserPhone().isBlank()) {
                Locator phoneField = page.locator("input[id*='phone'], input[name*='phone'], input[type='tel']").first();
                if (phoneField.count() > 0) {
                    phoneField.fill(config.getUserPhone());
                }
            }

            // Upload resume
            Locator resumeInput = page.locator("input[type='file']").first();
            if (resumeInput.count() > 0 && config.getResumePath() != null) {
                resumeInput.setInputFiles(Paths.get(config.getResumePath()));
            }

            // Submit
            Locator submitBtn = page.locator("input[type='submit'], button[type='submit'], button:has-text('Submit')").first();
            if (submitBtn.count() > 0) {
                submitBtn.click();
                page.waitForLoadState(LoadState.NETWORKIDLE);

                // Check for success confirmation
                String pageContent = page.content().toLowerCase();
                if (pageContent.contains("thank you") || pageContent.contains("application received")
                        || pageContent.contains("successfully submitted") || pageContent.contains("confirmation")) {
                    log.info("Greenhouse apply successful for job {}", jobId);
                    return ApplyResult.success(jobId, job.getCompanyName(), "GH-" + jobId);
                }
            }

            log.warn("Could not confirm Greenhouse submission for job {}", jobId);
            return ApplyResult.failure(jobId, job.getCompanyName(),
                "Form submitted but could not confirm success — check manually at: " + job.getJdUrl());

        } catch (Exception e) {
            log.error("Greenhouse apply failed for job {}", jobId, e);
            return ApplyResult.failure(jobId, job.getCompanyName(), "Automation error: " + e.getMessage());
        }
    }
}
