package com.hireloop.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.hireloop.model.Application;
import com.hireloop.model.Job;
import com.hireloop.repository.ApplicationRepository;
import com.hireloop.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GmailTrackerService {
    private static final String APPLICATION_NAME = "HireLoop";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = System.getProperty("user.home") + "/.hireloop";
    private static final String CREDENTIALS_FILE_PATH = TOKENS_DIRECTORY_PATH + "/credentials.json";

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    @Value("${gmail.user-email:}")
    private String userEmail;

    private Gmail gmailService;

    public GmailTrackerService(
            JobRepository jobRepository,
            ApplicationRepository applicationRepository) {
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
    }

    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void trackApplications() {
        try {
            if (gmailService == null) {
                gmailService = createGmailService();
            }

            int emailCount = 0;
            int classifiedCount = 0;
            int updatedCount = 0;

            // Run both tracking functions in parallel
            updatedCount += scanUserReplies();
            classifiedCount += scanApplicationEmails();
            emailCount = classifiedCount;

            System.out.println(String.format(
                "Scanned Gmail: %d new emails, classified %d, updated %d applications",
                emailCount, classifiedCount, updatedCount
            ));
        } catch (Exception e) {
            System.err.println("Error tracking applications: " + e.getMessage());
            gmailService = null;
        }
    }

    private int scanUserReplies() {
        try {
            String query = "subject:(APPLY- OR SKIP-)";
            List<Message> messages = searchEmails(query, 10);
            int updatedCount = 0;

            for (Message message : messages) {
                String body = getEmailBody(message);

                // Parse APPLY-N tokens
                Pattern applyPattern = Pattern.compile("APPLY-(\\d+)");
                Matcher applyMatcher = applyPattern.matcher(body);
                while (applyMatcher.find()) {
                    Integer jobId = Integer.parseInt(applyMatcher.group(1));
                    Optional<Job> jobOpt = jobRepository.findById(jobId);
                    if (jobOpt.isPresent()) {
                        Job job = jobOpt.get();
                        job.setConfirmed(true);
                        job.setStatus("CONFIRMED");
                        job.setUpdatedAt(LocalDateTime.now());
                        jobRepository.save(job);
                        updatedCount++;
                        System.out.println("Job " + jobId + " confirmed for application");
                    }
                }

                // Parse SKIP-N tokens
                Pattern skipPattern = Pattern.compile("SKIP-(\\d+)");
                Matcher skipMatcher = skipPattern.matcher(body);
                while (skipMatcher.find()) {
                    Integer jobId = Integer.parseInt(skipMatcher.group(1));
                    Optional<Job> jobOpt = jobRepository.findById(jobId);
                    if (jobOpt.isPresent()) {
                        Job job = jobOpt.get();
                        job.setStatus("SKIPPED");
                        job.setUpdatedAt(LocalDateTime.now());
                        jobRepository.save(job);
                        updatedCount++;
                        System.out.println("Job " + jobId + " marked as skipped");
                    }
                }
            }

            return updatedCount;
        } catch (Exception e) {
            System.err.println("Error scanning user replies: " + e.getMessage());
            return 0;
        }
    }

    private int scanApplicationEmails() {
        try {
            String query = "from:(google.com OR apple.com OR amazon.com OR aws.com OR meta.com OR microsoft.com OR " +
                         "ibm.com OR amd.com OR crowdstrike.com OR stripe.com OR tesla.com)";
            List<Message> messages = searchEmails(query, 50);
            int updatedCount = 0;

            for (Message message : messages) {
                String emailFrom = getEmailFrom(message);
                String subject = getEmailSubject(message);
                String body = getEmailBody(message);
                String threadId = message.getThreadId();

                // Classify email
                String classification = classifyEmail(subject, body);

                // Try to link to existing application or job
                List<Job> potentialJobs = jobRepository.findByCompanyNameContainingIgnoreCase(
                    extractCompanyName(emailFrom)
                );

                for (Job job : potentialJobs) {
                    Application app = applicationRepository.findByJobId(job.getId())
                            .stream()
                            .findFirst()
                            .orElse(null);

                    if (app == null) {
                        // Create new application
                        app = new Application();
                        app.setJob(job);
                    }

                    // Update application status
                    app.setPipelineStatus(classification);
                    app.setUpdatedAt(LocalDateTime.now());
                    applicationRepository.save(app);

                    System.out.println(String.format(
                        "Updated application for %s (%s) - Status: %s",
                        job.getCompanyName(), threadId, classification
                    ));
                    updatedCount++;
                }
            }

            return updatedCount;
        } catch (Exception e) {
            System.err.println("Error scanning application emails: " + e.getMessage());
            return 0;
        }
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

    private List<Message> searchEmails(String query, int maxResults) throws Exception {
        Gmail.Users.Messages.List request = gmailService.users().messages().list("me");
        request.setQ(query);
        request.setMaxResults((long) maxResults);
        return request.execute().getMessages() != null ?
               request.execute().getMessages() : Collections.emptyList();
    }

    private String getEmailBody(Message message) throws Exception {
        Message fullMessage = gmailService.users().messages().get("me", message.getId()).execute();

        if (fullMessage.getPayload() != null && fullMessage.getPayload().getParts() != null) {
            var part = fullMessage.getPayload().getParts().stream()
                    .filter(p -> "text/plain".equals(p.getMimeType()))
                    .findFirst();
            if (part.isPresent() && part.get().getBody() != null) {
                return part.get().getBody().getData() != null ?
                       new String(part.get().getBody().getData()) : "";
            }
        }
        return "";
    }

    private String getEmailFrom(Message message) throws Exception {
        Message fullMessage = gmailService.users().messages().get("me", message.getId()).execute();
        var headers = fullMessage.getPayload().getHeaders();
        return headers.stream()
                .filter(h -> "From".equalsIgnoreCase(h.getName()))
                .findFirst()
                .map(h -> h.getValue())
                .orElse("");
    }

    private String getEmailSubject(Message message) throws Exception {
        Message fullMessage = gmailService.users().messages().get("me", message.getId()).execute();
        var headers = fullMessage.getPayload().getHeaders();
        return headers.stream()
                .filter(h -> "Subject".equalsIgnoreCase(h.getName()))
                .findFirst()
                .map(h -> h.getValue())
                .orElse("");
    }

    private Gmail createGmailService() throws Exception {
        if (!new File(CREDENTIALS_FILE_PATH).exists()) {
            System.out.println("Gmail credentials not found at " + CREDENTIALS_FILE_PATH);
            System.out.println("Please set up OAuth2 credentials for Gmail API");
            return null;
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(new FileInputStream(CREDENTIALS_FILE_PATH)));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, clientSecrets,
                Collections.singletonList("https://www.googleapis.com/auth/gmail.readonly"))
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

        return new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void initializeGmail() {
        try {
            gmailService = createGmailService();
            System.out.println("Gmail service initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize Gmail service: " + e.getMessage());
        }
    }
}
