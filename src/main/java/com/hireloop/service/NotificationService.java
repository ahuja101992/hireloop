package com.hireloop.service;

import com.hireloop.model.Job;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class NotificationService {
    private final Optional<JavaMailSender> mailSender;
    private final List<ResumeChangeNotification> digestQueue = Collections.synchronizedList(new ArrayList<>());

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public NotificationService(Optional<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    public void notifyNewJob(Job job, String recipientEmail) {
        if (mailSender.isEmpty()) {
            System.out.println("Email disabled - would notify: " + recipientEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject("New Job Opportunity: " + job.getTitle());

            boolean resumeNeedsChanges = job.getTailoredResumeJson() != null && !job.getTailoredResumeJson().isEmpty();
            String resumeStatus = resumeNeedsChanges ? "⚠️ Resume needs tailoring" : "✓ Resume is a good match";

            message.setText(String.format("""
                    A new job opportunity has been found:

                    Company: %s
                    Title: %s
                    Fit Score: %s
                    Resume Status: %s

                    Job URL: %s
                    View in HireLoop: %s/dashboard/jobs/%d
                    """, job.getCompanyName(), job.getTitle(), job.getFitScore(), resumeStatus,
                    job.getJdUrl(), baseUrl, job.getId()));

            mailSender.get().send(message);
        } catch (Exception e) {
            System.err.println("Error sending notification: " + e.getMessage());
        }
    }

    public void notifyReadinessReport(String companyName, String report, String recipientEmail) {
        if (mailSender.isEmpty()) {
            System.out.println("Email disabled - would send report for: " + companyName);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject("Interview Prep Readiness: " + companyName);
            message.setText(report);
            mailSender.get().send(message);
        } catch (Exception e) {
            System.err.println("Error sending readiness report: " + e.getMessage());
        }
    }

    public void notifyResumeChange(Job job, String magnitude, int changeCount, String recipientEmail) {
        if ("MAJOR".equals(magnitude)) {
            sendImmediateResumeChangeEmail(job, changeCount, recipientEmail);
        } else {
            queueForDailyDigest(job, changeCount);
        }
    }

    private void sendImmediateResumeChangeEmail(Job job, int changeCount, String recipientEmail) {
        if (mailSender.isEmpty()) {
            System.out.println("Email disabled - would send immediate email for job: " + job.getId());
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject("Resume Needs Major Changes: " + job.getCompanyName());
            message.setText(String.format("""
                    Resume requires MAJOR changes for this opportunity:

                    Company: %s
                    Title: %s
                    Changes Required: %d

                    Review the suggested changes before applying.
                    Review diff: /dashboard/jobs/%d/diff
                    """, job.getCompanyName(), job.getTitle(), changeCount, job.getId()));

            mailSender.get().send(message);
            System.out.println("Sent immediate resume change email for job " + job.getId());
        } catch (Exception e) {
            System.err.println("Error sending immediate email: " + e.getMessage());
        }
    }

    private void queueForDailyDigest(Job job, int changeCount) {
        ResumeChangeNotification notification = new ResumeChangeNotification();
        notification.setJobId(job.getId());
        notification.setCompanyName(job.getCompanyName());
        notification.setTitle(job.getTitle());
        notification.setScore(job.getFitScore() != null ? job.getFitScore().intValue() : 0);
        notification.setChangeCount(changeCount);
        digestQueue.add(notification);
    }

    public void sendDailyDigest(String recipientEmail) {
        if (digestQueue.isEmpty() || mailSender.isEmpty()) {
            System.out.println("No digest items or email disabled");
            return;
        }

        try {
            StringBuilder digestBody = new StringBuilder();
            digestBody.append("Daily Resume Change Digest\n");
            digestBody.append("===========================\n\n");

            for (ResumeChangeNotification notification : digestQueue) {
                digestBody.append(String.format("[%s] %s — Score %d — %d changes\n",
                    notification.getCompanyName(),
                    notification.getTitle(),
                    notification.getScore(),
                    notification.getChangeCount()
                ));
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject("Resume Changes Digest - " + new Date());
            message.setText(digestBody.toString());

            mailSender.get().send(message);
            System.out.println("Sent daily digest with " + digestQueue.size() + " items");
            digestQueue.clear();
        } catch (Exception e) {
            System.err.println("Error sending digest: " + e.getMessage());
        }
    }

    public void notifyApplicationSubmitted(Job job, String recipientEmail) {
        if (mailSender.isEmpty()) {
            System.out.println("[NOTIFY] Application submitted: " + job.getCompanyName() + " - " + job.getTitle());
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject("✅ Application Submitted: " + job.getCompanyName() + " - " + job.getTitle());
            message.setText(String.format("""
                    Your application has been submitted!

                    Company: %s
                    Position: %s
                    Fit Score: %s
                    Job URL: %s

                    View in HireLoop: %s/dashboard/jobs/%d
                    """, job.getCompanyName(), job.getTitle(), job.getFitScore(), job.getJdUrl(), baseUrl, job.getId()));
            mailSender.get().send(message);
        } catch (Exception e) {
            System.err.println("Error sending submission confirmation: " + e.getMessage());
        }
    }

    public void notifyApplyFailed(Job job, String errorMessage, String recipientEmail) {
        if (mailSender.isEmpty()) {
            System.out.println("[NOTIFY] Apply failed: " + job.getCompanyName() + " - " + errorMessage);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipientEmail);
            message.setSubject("⚠️ Manual Apply Needed: " + job.getCompanyName() + " - " + job.getTitle());
            message.setText(String.format("""
                    HireLoop could not auto-submit your application. Please apply manually.

                    Company: %s
                    Position: %s
                    Fit Score: %s
                    Job URL: %s

                    View in HireLoop: %s/dashboard/jobs/%d

                    Reason: %s
                    """, job.getCompanyName(), job.getTitle(), job.getFitScore(), job.getJdUrl(), baseUrl, job.getId(), errorMessage));
            mailSender.get().send(message);
        } catch (Exception e) {
            System.err.println("Error sending failure notification: " + e.getMessage());
        }
    }

    public List<ResumeChangeNotification> getDigestQueue() {
        return new ArrayList<>(digestQueue);
    }

    // Inner class for digest tracking
    public static class ResumeChangeNotification {
        private Integer jobId;
        private String companyName;
        private String title;
        private int score;
        private int changeCount;

        public Integer getJobId() { return jobId; }
        public void setJobId(Integer jobId) { this.jobId = jobId; }

        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }

        public int getChangeCount() { return changeCount; }
        public void setChangeCount(int changeCount) { this.changeCount = changeCount; }
    }
}
