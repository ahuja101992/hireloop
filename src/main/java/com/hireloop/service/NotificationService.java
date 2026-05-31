package com.hireloop.service;

import com.hireloop.model.Job;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class NotificationService {
    private final Optional<JavaMailSender> mailSender;

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
            message.setText(String.format("""
                    A new job opportunity has been found:

                    Company: %s
                    Title: %s
                    Fit Score: %s
                    URL: %s
                    """, job.getCompanyName(), job.getTitle(), job.getFitScore(), job.getJdUrl()));

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
}
