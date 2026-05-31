package com.hireloop.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "http://localhost:3000")
public class ResumeController {
    private static final String RESUME_PATH = "resume/resume.docx";

    @GetMapping("/status")
    public Map<String, Object> getResumeStatus() {
        try {
            boolean exists = Files.exists(Paths.get(RESUME_PATH));
            long size = exists ? Files.size(Paths.get(RESUME_PATH)) : 0;
            return Map.of(
                "exists", exists,
                "size", size,
                "path", RESUME_PATH
            );
        } catch (Exception e) {
            return Map.of(
                "exists", false,
                "error", e.getMessage()
            );
        }
    }

    @PostMapping("/upload")
    public Map<String, Object> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Map.of("success", false, "error", "File is empty");
            }

            Files.write(Paths.get(RESUME_PATH), file.getBytes());
            return Map.of(
                "success", true,
                "filename", file.getOriginalFilename(),
                "size", file.getSize(),
                "message", "Resume uploaded successfully"
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
}
