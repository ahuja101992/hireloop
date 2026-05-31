package com.hireloop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "job", indexes = {
    @Index(name = "idx_job_company", columnList = "company_name"),
    @Index(name = "idx_job_status", columnList = "status")
})
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String companyName;
    private String title;
    private String jdUrl;

    @Column(columnDefinition = "TEXT")
    private String jdText;

    private BigDecimal fitScore;
    private String status = "PENDING";
    private Boolean confirmed = false;

    @Column(columnDefinition = "TEXT")
    private String tailoredResumeJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getJdUrl() { return jdUrl; }
    public void setJdUrl(String jdUrl) { this.jdUrl = jdUrl; }

    public String getJdText() { return jdText; }
    public void setJdText(String jdText) { this.jdText = jdText; }

    public BigDecimal getFitScore() { return fitScore; }
    public void setFitScore(BigDecimal fitScore) { this.fitScore = fitScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getConfirmed() { return confirmed; }
    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }

    public String getTailoredResumeJson() { return tailoredResumeJson; }
    public void setTailoredResumeJson(String tailoredResumeJson) { this.tailoredResumeJson = tailoredResumeJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
