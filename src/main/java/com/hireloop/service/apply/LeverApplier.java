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
public class LeverApplier implements AtsApplier {
    private static final Logger log = LoggerFactory.getLogger(LeverApplier.class);

    @Override
    public boolean supports(String atsType) {
        return "lever".equalsIgnoreCase(atsType);
    }

    @Override
    public ApplyResult apply(Job job, ResumeMaster resume, ApplyConfig config) {
        String jobId = String.valueOf(job.getId());
        log.info("Lever apply: job={} company={} url={}", jobId, job.getCompanyName(), job.getJdUrl());

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(config.isHeadless())
            );
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            // Lever apply URL is typically /apply appended to job URL
            String applyUrl = job.getJdUrl();
            if (!applyUrl.endsWith("/apply")) {
                applyUrl = applyUrl.endsWith("/") ? applyUrl + "apply" : applyUrl + "/apply";
            }

            page.navigate(applyUrl);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Fill name
            Locator nameField = page.locator("input[name='name'], input[id='name']").first();
            if (nameField.count() > 0) {
                nameField.fill(config.getUserName());
            }

            // Fill email
            Locator emailField = page.locator("input[name='email'], input[type='email']").first();
            if (emailField.count() > 0) {
                emailField.fill(config.getUserEmail());
            }

            // Fill phone
            if (config.getUserPhone() != null && !config.getUserPhone().isBlank()) {
                Locator phoneField = page.locator("input[name='phone'], input[type='tel']").first();
                if (phoneField.count() > 0) {
                    phoneField.fill(config.getUserPhone());
                }
            }

            // Upload resume
            Locator resumeInput = page.locator("input[type='file']").first();
            if (resumeInput.count() > 0 && config.getResumePath() != null) {
                resumeInput.setInputFiles(Paths.get(config.getResumePath()));
                page.waitForTimeout(1500);
            }

            // Submit
            Locator submitBtn = page.locator("button[type='submit'], input[type='submit']").first();
            if (submitBtn.count() > 0) {
                submitBtn.click();
                page.waitForLoadState(LoadState.NETWORKIDLE);

                String pageContent = page.content().toLowerCase();
                if (pageContent.contains("thank you") || pageContent.contains("application received")
                        || pageContent.contains("successfully") || pageContent.contains("submitted")) {
                    log.info("Lever apply successful for job {}", jobId);
                    return ApplyResult.success(jobId, job.getCompanyName(), "LEVER-" + jobId);
                }
            }

            return ApplyResult.failure(jobId, job.getCompanyName(),
                "Could not confirm Lever submission — check manually at: " + applyUrl);

        } catch (Exception e) {
            log.error("Lever apply failed for job {}", jobId, e);
            return ApplyResult.failure(jobId, job.getCompanyName(), "Automation error: " + e.getMessage());
        }
    }
}
