package com.hireloop.service;

import com.hireloop.model.Application;
import com.hireloop.model.Job;
import com.hireloop.repository.ApplicationRepository;
import com.hireloop.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GmailTrackerService {
    private static final String TOKENS_DIRECTORY_PATH = System.getProperty("user.home") + "/.hireloop";
    private static final String CREDENTIALS_FILE_PATH = TOKENS_DIRECTORY_PATH + "/credentials.json";

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    @Value("${gmail.user-email:}")
    private String userEmail;

    private boolean gmailInitialized = false;

    public GmailTrackerService(
            JobRepository jobRepository,
            ApplicationRepository applicationRepository) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
    }

    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void trackApplications() {
        if (!gmailInitialized) {
            System.out.println("Gmail service not initialized. Configure credentials at " + CREDENTIALS_FILE_PATH);
            return;
        }

        try {
            int updatedCount = 0;
            int emailCount = 0;

            // Run both tracking functions
            updatedCount += scanUserReplies();
            updatedCount += scanApplicationEmails();
            emailCount = updatedCount;

            System.out.println(String.format(
                "Scanned Gmail: %d new emails, classified %d, updated %d applications",
                emailCount, emailCount, updatedCount
            ));
        } catch (Exception e) {
            System.err.println("Error tracking applications: " + e.getMessage());
        }
    }

    private int scanUserReplies() {
        // Stub implementation - parses email body for APPLY-N and SKIP-N tokens
        // Full implementation requires Gmail API OAuth2 setup
        int updatedCount = 0;

        try {
            // In production, this would:
            // 1. Connect to Gmail API
            // 2. Search for emails with subject containing "APPLY-" or "SKIP-"
            // 3. Parse email bodies for job IDs
            // 4. Update job status accordingly

            System.out.println("User reply scanning requires Gmail API configuration");
        } catch (Exception e) {
            System.err.println("Error scanning user replies: " + e.getMessage());
        }

        return updatedCount;
    }

    private int scanApplicationEmails() {
        // Stub implementation - scans inbox for application status updates
        // Full implementation requires Gmail API OAuth2 setup
        int updatedCount = 0;

        try {
            // In production, this would:
            // 1. Connect to Gmail API
            // 2. Search for emails from tech company domains
            // 3. Classify each email by content (interview, offer, rejection, etc.)
            // 4. Create or update Application records
            // 5. Link to existing Job records

            System.out.println("Application email scanning requires Gmail API configuration");
        } catch (Exception e) {
            System.err.println("Error scanning application emails: " + e.getMessage());
        }

        return updatedCount;
    }

    private String classifyEmail(String subject, String body) {
        String combined = (subject + " " + body).toLowerCase();

        if (combined.contains("interview") || combined.contains("schedule")) {
            return "INTERVIEW";
        } else if (combined.contains("offer") || combined.contains("congratulations")) {
            return "OFFER";
        } else if (combined.contains("unfortunately") || combined.contains("not selected") ||
                   combined.contains("rejection") || combined.contains("rejected")) {
            return "REJECTED";
        } else if (combined.contains("received your application") || combined.contains("application received")) {
            return "ACKNOWLEDGED";
        } else {
            return "RECRUITER_SCREEN";
        }
    }

    private String extractCompanyName(String emailFrom) {
        String[] parts = emailFrom.split("@");
        if (parts.length > 1) {
            return parts[1].split("\\.")[0].toUpperCase();
        }
        return emailFrom;
    }

    public void initializeGmail() {
        try {
            System.out.println("Gmail tracker initialized (stub mode)");
            System.out.println("To enable full functionality, configure Gmail API credentials at: " + CREDENTIALS_FILE_PATH);
            gmailInitialized = true;
        } catch (Exception e) {
            System.err.println("Failed to initialize Gmail service: " + e.getMessage());
        }
    }

    public void updateApplicationFromEmail(Integer jobId, String status) {
        Optional<Job> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            Job job = jobOpt.get();
            Application app = applicationRepository.findByJobId(jobId)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (app == null) {
                app = new Application();
                app.setJob(job);
            }

            app.setPipelineStatus(status);
            app.setUpdatedAt(LocalDateTime.now());
            applicationRepository.save(app);
        }
    }
}
