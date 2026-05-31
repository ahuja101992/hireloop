package com.hireloop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hireloop.model.Job;
import com.hireloop.provider.LlmProvider;
import com.hireloop.provider.LlmProviderFactory;
import com.hireloop.repository.JobRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ResumeAdapterService {
    private final JobRepository jobRepository;
    private final LlmProviderFactory llmProviderFactory;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeAdapterService(
            JobRepository jobRepository,
            LlmProviderFactory llmProviderFactory,
            NotificationService notificationService) {
        this.jobRepository = jobRepository;
        this.llmProviderFactory = llmProviderFactory;
        this.notificationService = notificationService;
    }

    public ResumeAdaptationResult adaptResumeForJob(Integer jobId, String baseResumeJson) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        LlmProvider provider = llmProviderFactory.getProvider();
        JsonNode fitScorerResponse = provider.adaptResume(baseResumeJson, job.getJdText());

        // Parse the response
        JsonNode matchingPoints = fitScorerResponse.get("matching_points");
        JsonNode gaps = fitScorerResponse.get("gaps");
        JsonNode resumeChanges = fitScorerResponse.get("resume_changes");

        // Apply changes to resume JSON
        ObjectNode modifiedResume = null;
        try {
            modifiedResume = (ObjectNode) objectMapper.readTree(baseResumeJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse resume JSON: " + e.getMessage(), e);
        }
        int changeCount = applyResumeChanges(modifiedResume, resumeChanges);

        // Determine change magnitude
        String magnitude = changeCount > 15 ? "MAJOR" : "MINOR";

        // Store tailored resume
        job.setTailoredResumeJson(modifiedResume.toString());
        job.setStatus("TAILORED");
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);

        // Render to DOCX (optional)
        try {
            String outputPath = "/tmp/resume_" + jobId + ".docx";
            renderResumeToDocx(modifiedResume, outputPath);
        } catch (Exception e) {
            System.err.println("Warning: Could not render DOCX: " + e.getMessage());
        }

        ResumeAdaptationResult result = new ResumeAdaptationResult();
        result.setJobId(jobId);
        result.setChangeCount(changeCount);
        result.setMagnitude(magnitude);
        result.setMatchingPoints(matchingPoints != null ? matchingPoints.asText() : "");
        result.setGaps(gaps != null ? gaps.asText() : "");

        return result;
    }

    private int applyResumeChanges(ObjectNode resume, JsonNode changes) {
        int changeCount = 0;

        if (changes == null || !changes.isObject()) {
            return changeCount;
        }

        // Apply each suggested change from Claude
        Iterator<String> fieldNames = changes.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            JsonNode changeValue = changes.get(field);

            if (changeValue.isArray()) {
                resume.set(field, changeValue);
                changeCount += changeValue.size();
            } else if (changeValue.isTextual()) {
                resume.put(field, changeValue.asText());
                changeCount++;
            }
        }

        return changeCount;
    }

    private void renderResumeToDocx(JsonNode resumeJson, String outputPath) throws Exception {
        XWPFDocument document = new XWPFDocument();

        // Add name
        if (resumeJson.has("name")) {
            addParagraph(document, resumeJson.get("name").asText(), true);
        }

        // Add contact info
        if (resumeJson.has("email")) {
            addParagraph(document, resumeJson.get("email").asText(), false);
        }
        if (resumeJson.has("phone")) {
            addParagraph(document, resumeJson.get("phone").asText(), false);
        }

        // Add summary
        if (resumeJson.has("summary")) {
            addParagraph(document, "SUMMARY", true);
            addParagraph(document, resumeJson.get("summary").asText(), false);
        }

        // Add experience
        if (resumeJson.has("experience")) {
            addParagraph(document, "EXPERIENCE", true);
            JsonNode experience = resumeJson.get("experience");
            if (experience.isArray()) {
                for (JsonNode exp : experience) {
                    addParagraph(document, exp.get("title").asText(), false);
                    if (exp.has("details")) {
                        addParagraph(document, exp.get("details").asText(), false);
                    }
                }
            }
        }

        // Add skills
        if (resumeJson.has("skills")) {
            addParagraph(document, "SKILLS", true);
            JsonNode skills = resumeJson.get("skills");
            if (skills.isArray()) {
                StringBuilder skillsText = new StringBuilder();
                for (JsonNode skill : skills) {
                    skillsText.append(skill.asText()).append(", ");
                }
                addParagraph(document, skillsText.toString(), false);
            }
        }

        // Add education
        if (resumeJson.has("education")) {
            addParagraph(document, "EDUCATION", true);
            JsonNode education = resumeJson.get("education");
            if (education.isArray()) {
                for (JsonNode edu : education) {
                    addParagraph(document, edu.get("degree").asText(), false);
                }
            }
        }

        try (FileOutputStream out = new FileOutputStream(outputPath)) {
            document.write(out);
        }
        document.close();
    }

    private void addParagraph(XWPFDocument document, String text, boolean bold) {
        XWPFParagraph paragraph = document.createParagraph();
        if (text != null && !text.isEmpty()) {
            if (bold) {
                paragraph.createRun().setText(text);
                paragraph.getRuns().forEach(run -> run.setBold(true));
            } else {
                paragraph.createRun().setText(text);
            }
        }
    }

    public String getTailoredResume(Integer jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        return job.getTailoredResumeJson();
    }

    // Inner class for result
    public static class ResumeAdaptationResult {
        private Integer jobId;
        private int changeCount;
        private String magnitude; // MINOR or MAJOR
        private String matchingPoints;
        private String gaps;

        public Integer getJobId() { return jobId; }
        public void setJobId(Integer jobId) { this.jobId = jobId; }

        public int getChangeCount() { return changeCount; }
        public void setChangeCount(int changeCount) { this.changeCount = changeCount; }

        public String getMagnitude() { return magnitude; }
        public void setMagnitude(String magnitude) { this.magnitude = magnitude; }

        public String getMatchingPoints() { return matchingPoints; }
        public void setMatchingPoints(String matchingPoints) { this.matchingPoints = matchingPoints; }

        public String getGaps() { return gaps; }
        public void setGaps(String gaps) { this.gaps = gaps; }
    }
}
