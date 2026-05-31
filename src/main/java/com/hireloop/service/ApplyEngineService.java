package com.hireloop.service;

import com.hireloop.model.Application;
import com.hireloop.model.Job;
import com.hireloop.model.ResumeMaster;
import com.hireloop.repository.ApplicationRepository;
import com.hireloop.repository.JobRepository;
import com.hireloop.repository.ResumeMasterRepository;
import com.hireloop.service.apply.ApplyConfig;
import com.hireloop.service.apply.ApplyResult;
import com.hireloop.service.apply.AtsApplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApplyEngineService {
    private static final Logger log = LoggerFactory.getLogger(ApplyEngineService.class);

    @Value("${apply-engine.enabled:false}")
    private boolean autoApplyEnabled;

    @Value("${apply-engine.headless:true}")
    private boolean headless;

    @Value("${apply-engine.user-name:}")
    private String userName;

    @Value("${apply-engine.user-email:${gmail.user-email:}}")
    private String userEmail;

    @Value("${apply-engine.user-phone:}")
    private String userPhone;

    @Value("${apply-engine.resume-path:resume/resume.docx}")
    private String resumePath;

    private final List<AtsApplier> appliers;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeMasterRepository resumeMasterRepository;
    private final ResumeAdapterService resumeAdapterService;
    private final NotificationService notificationService;

    public ApplyEngineService(
            List<AtsApplier> appliers,
            JobRepository jobRepository,
            ApplicationRepository applicationRepository,
            ResumeMasterRepository resumeMasterRepository,
            ResumeAdapterService resumeAdapterService,
            NotificationService notificationService) {
        this.appliers = appliers;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.resumeMasterRepository = resumeMasterRepository;
        this.resumeAdapterService = resumeAdapterService;
        this.notificationService = notificationService;
    }

    public ApplyResult applyToJob(Integer jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        if (Boolean.FALSE.equals(job.getConfirmed())) {
            return ApplyResult.failure(String.valueOf(jobId), job.getCompanyName(),
                "Job not confirmed for apply — call confirm-apply first");
        }
        if ("APPLIED".equals(job.getStatus())) {
            return ApplyResult.failure(String.valueOf(jobId), job.getCompanyName(), "Already applied to this job");
        }

        // Tailor resume if not done yet
        ResumeMaster resume = resumeMasterRepository.findAll().stream()
            .max((a, b) -> a.getId().compareTo(b.getId()))
            .orElse(null);

        if (!"TAILORED".equals(job.getStatus()) && resume != null) {
            try {
                resumeAdapterService.adaptResumeForJob(jobId, resume.getResumeJson());
                job = jobRepository.findById(jobId).orElse(job);
            } catch (Exception e) {
                log.warn("Resume tailoring failed for job {}, proceeding with base resume: {}", jobId, e.getMessage());
            }
        }

        // Capture final reference for lambda
        final Job finalJob = job;

        // Find ATS strategy — prefer exact match, fall back to custom
        AtsApplier applier = appliers.stream()
            .filter(a -> a.supports(finalJob.getAtsType()) && !"custom".equalsIgnoreCase(finalJob.getAtsType()))
            .findFirst()
            .orElseGet(() -> appliers.stream()
                .filter(a -> a.supports("custom"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No applier found for ATS: " + finalJob.getAtsType())));

        ApplyConfig config = new ApplyConfig(userName, userEmail, userPhone, resumePath, headless);
        ApplyResult result = applier.apply(finalJob, resume, config);

        // Update job + create Application record
        if (result.isSuccess()) {
            job.setStatus("APPLIED");
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);

            Application application = new Application();
            application.setJob(job);
            application.setPipelineStatus("APPLIED");
            application.setUpdatedAt(LocalDateTime.now());
            applicationRepository.save(application);

            notificationService.notifyApplicationSubmitted(job, userEmail);
            log.info("Successfully applied to job {} at {}", jobId, job.getCompanyName());
        } else {
            job.setStatus("APPLY_FAILED");
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);

            notificationService.notifyApplyFailed(job, result.getErrorMessage(), userEmail);
            log.warn("Apply failed for job {} at {}: {}", jobId, job.getCompanyName(), result.getErrorMessage());
        }

        return result;
    }

    public List<ApplyResult> applyBatch() {
        List<Job> confirmedJobs = jobRepository.findByConfirmedTrueAndStatusIn(List.of("SCORED", "TAILORED"));
        log.info("Batch apply: {} confirmed jobs to process", confirmedJobs.size());

        List<ApplyResult> results = new ArrayList<>();
        for (Job job : confirmedJobs) {
            try {
                results.add(applyToJob(job.getId()));
            } catch (Exception e) {
                log.error("Batch apply error for job {}: {}", job.getId(), e.getMessage());
                results.add(ApplyResult.failure(String.valueOf(job.getId()), job.getCompanyName(), e.getMessage()));
            }
        }
        return results;
    }

    public boolean isAutoApplyEnabled() {
        return autoApplyEnabled;
    }

    public void setAutoApplyEnabled(boolean enabled) {
        this.autoApplyEnabled = enabled;
        log.info("Auto-apply feature flag set to: {}", enabled);
    }

    public long getPendingConfirmedCount() {
        return jobRepository.findByConfirmedTrueAndStatusIn(List.of("SCORED", "TAILORED")).size();
    }
}
