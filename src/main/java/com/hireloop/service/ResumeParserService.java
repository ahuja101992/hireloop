package com.hireloop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireloop.model.ResumeMaster;
import com.hireloop.repository.ResumeMasterRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ResumeParserService {
    private final ResumeMasterRepository resumeMasterRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeParserService(ResumeMasterRepository resumeMasterRepository) {
        this.resumeMasterRepository = resumeMasterRepository;
    }

    public ResumeMaster parseResume(String resumePath, Integer userId) {
        try {
            Map<String, Object> resumeJson = parseDocxResume(resumePath);

            ResumeMaster resumeMaster = new ResumeMaster();
            resumeMaster.setUserId(userId);
            resumeMaster.setResumeJson(objectMapper.writeValueAsString(resumeJson));
            resumeMaster.setUpdatedAt(LocalDateTime.now());

            return resumeMasterRepository.save(resumeMaster);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse resume: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> parseDocxResume(String filePath) {
        Map<String, Object> resumeData = new LinkedHashMap<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            List<String> allText = new ArrayList<>();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText().trim();
                if (!text.isEmpty()) {
                    allText.add(text);
                }
            }

            // Parse resume sections
            resumeData.put("name", extractName(allText));
            resumeData.put("email", extractEmail(allText));
            resumeData.put("phone", extractPhone(allText));
            resumeData.put("summary", extractSummary(allText));
            resumeData.put("experience", extractExperience(allText));
            resumeData.put("skills", extractSkills(allText));
            resumeData.put("education", extractEducation(allText));

        } catch (Exception e) {
            System.err.println("Error parsing DOCX: " + e.getMessage());
        }

        return resumeData;
    }

    private String extractName(List<String> lines) {
        // Usually first line or line before email
        if (!lines.isEmpty()) {
            return lines.get(0);
        }
        return "Unknown";
    }

    private String extractEmail(List<String> lines) {
        for (String line : lines) {
            if (line.contains("@")) {
                return line.trim();
            }
        }
        return "";
    }

    private String extractPhone(List<String> lines) {
        for (String line : lines) {
            if (line.matches(".*\\d{3}.*\\d{3}.*\\d{4}.*")) {
                return line.trim();
            }
        }
        return "";
    }

    private String extractSummary(List<String> lines) {
        // Find SUMMARY or PROFILE section
        StringBuilder summary = new StringBuilder();
        boolean inSummary = false;

        for (String line : lines) {
            if (line.toUpperCase().contains("SUMMARY") || line.toUpperCase().contains("PROFILE")) {
                inSummary = true;
                continue;
            }

            if (inSummary) {
                if (line.toUpperCase().matches(".*EXPERIENCE|EDUCATION|SKILLS.*")) {
                    break;
                }
                summary.append(line).append(" ");
            }
        }

        return summary.toString().trim();
    }

    private List<Map<String, String>> extractExperience(List<String> lines) {
        List<Map<String, String>> experience = new ArrayList<>();
        boolean inExperience = false;
        Map<String, String> currentExp = null;

        for (String line : lines) {
            if (line.toUpperCase().contains("EXPERIENCE")) {
                inExperience = true;
                continue;
            }

            if (inExperience) {
                if (line.toUpperCase().matches(".*EDUCATION|SKILLS|CERTIFICATIONS.*")) {
                    if (currentExp != null) {
                        experience.add(currentExp);
                    }
                    break;
                }

                // Detect new job entry (usually title and company together)
                if (line.matches(".*[A-Z].*")) {
                    if (currentExp != null) {
                        experience.add(currentExp);
                    }
                    currentExp = new LinkedHashMap<>();
                    currentExp.put("title", line);
                    currentExp.put("details", "");
                } else if (currentExp != null && !line.isEmpty()) {
                    String details = currentExp.getOrDefault("details", "");
                    currentExp.put("details", details + line + " ");
                }
            }
        }

        if (currentExp != null) {
            experience.add(currentExp);
        }

        return experience;
    }

    private List<String> extractSkills(List<String> lines) {
        List<String> skills = new ArrayList<>();
        boolean inSkills = false;

        for (String line : lines) {
            if (line.toUpperCase().contains("SKILLS") || line.toUpperCase().contains("TECHNOLOGIES")) {
                inSkills = true;
                continue;
            }

            if (inSkills) {
                if (line.toUpperCase().matches(".*EXPERIENCE|EDUCATION|CERTIFICATIONS.*")) {
                    break;
                }

                // Split by comma, pipe, or semicolon
                String[] parts = line.split("[,;|]");
                for (String skill : parts) {
                    String trimmed = skill.trim();
                    if (!trimmed.isEmpty()) {
                        skills.add(trimmed);
                    }
                }
            }
        }

        return skills;
    }

    private List<Map<String, String>> extractEducation(List<String> lines) {
        List<Map<String, String>> education = new ArrayList<>();
        boolean inEducation = false;

        for (String line : lines) {
            if (line.toUpperCase().contains("EDUCATION")) {
                inEducation = true;
                continue;
            }

            if (inEducation && !line.isEmpty()) {
                Map<String, String> edu = new LinkedHashMap<>();
                edu.put("degree", line);
                education.add(edu);
            }
        }

        return education;
    }

    public ResumeMaster getLatestResume(Integer userId) {
        return resumeMasterRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .findFirst()
                .orElse(null);
    }
}
