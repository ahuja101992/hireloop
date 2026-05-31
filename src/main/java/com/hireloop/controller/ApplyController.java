package com.hireloop.controller;

import com.hireloop.service.ApplyEngineService;
import com.hireloop.service.apply.ApplyResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apply")
@CrossOrigin(origins = "http://localhost:3000")
public class ApplyController {
    private final ApplyEngineService applyEngineService;

    public ApplyController(ApplyEngineService applyEngineService) {
        this.applyEngineService = applyEngineService;
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> applyToJob(@PathVariable Integer jobId) {
        ApplyResult result = applyEngineService.applyToJob(jobId);
        return ResponseEntity.ok(Map.of(
            "success", result.isSuccess(),
            "jobId", result.getJobId(),
            "companyName", result.getCompanyName(),
            "message", result.isSuccess()
                ? "Application submitted successfully (appId=" + result.getApplicationId() + ")"
                : result.getErrorMessage()
        ));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> applyBatch() {
        List<ApplyResult> results = applyEngineService.applyBatch();
        long succeeded = results.stream().filter(ApplyResult::isSuccess).count();
        long failed = results.size() - succeeded;
        return ResponseEntity.ok(Map.of(
            "total", results.size(),
            "succeeded", succeeded,
            "failed", failed,
            "results", results.stream().map(r -> Map.of(
                "success", r.isSuccess(),
                "jobId", r.getJobId(),
                "companyName", r.getCompanyName(),
                "message", r.isSuccess() ? "Applied" : r.getErrorMessage()
            )).toList()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "autoApplyEnabled", applyEngineService.isAutoApplyEnabled(),
            "pendingConfirmedJobs", applyEngineService.getPendingConfirmedCount()
        ));
    }

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleAutoApply() {
        boolean newState = !applyEngineService.isAutoApplyEnabled();
        applyEngineService.setAutoApplyEnabled(newState);
        return ResponseEntity.ok(Map.of(
            "autoApplyEnabled", newState,
            "message", "Auto-apply " + (newState ? "ENABLED" : "DISABLED")
        ));
    }
}
