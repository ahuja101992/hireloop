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
public class WorkdayApplier implements AtsApplier {
    private static final Logger log = LoggerFactory.getLogger(WorkdayApplier.class);

    @Override
    public boolean supports(String atsType) {
        return "workday".equalsIgnoreCase(atsType);
    }

    @Override
    public ApplyResult apply(Job job, ResumeMaster resume, ApplyConfig config) {
        String jobId = String.valueOf(job.getId());
        log.info("Workday apply: job={} company={} url={}", jobId, job.getCompanyName(), job.getJdUrl());

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(config.isHeadless())
            );
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.navigate(job.getJdUrl());
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Workday: click Apply button
            Locator applyBtn = page.locator("a[data-automation-id='applyButton'], button:has-text('Apply'), a:has-text('Apply Now')").first();
            if (applyBtn.count() > 0) {
                applyBtn.click();
                page.waitForLoadState(LoadState.NETWORKIDLE);
                page.waitForTimeout(2000);
            }

            // Workday create account or autofill page
            // Try to fill email for account creation
            Locator emailField = page.locator("input[data-automation-id='email'], input[type='email']").first();
            if (emailField.count() > 0) {
                emailField.fill(config.getUserEmail());
            }

            // Upload resume if file input is present (Step 1 of Workday often has resume upload)
            Locator resumeInput = page.locator("input[type='file']").first();
            if (resumeInput.count() > 0 && config.getResumePath() != null) {
                resumeInput.setInputFiles(Paths.get(config.getResumePath()));
                page.waitForTimeout(2000);
            }

            // Workday multi-step — too complex for full automation
            // Return manual needed after initial steps
            log.warn("Workday multi-step flow detected for job {} — manual completion required", jobId);
            return ApplyResult.failure(jobId, job.getCompanyName(),
                "Workday requires manual completion. Browser opened with form partially filled. Apply at: " + job.getJdUrl());

        } catch (Exception e) {
            log.error("Workday apply failed for job {}", jobId, e);
            return ApplyResult.failure(jobId, job.getCompanyName(), "Automation error: " + e.getMessage());
        }
    }
}
