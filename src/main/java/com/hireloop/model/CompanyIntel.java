package com.hireloop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_intel", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"company_name", "source"})
})
public class CompanyIntel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String companyName;
    private String source;

    @Column(columnDefinition = "TEXT")
    private String rawData;

    @Column(columnDefinition = "TEXT")
    private String interviewRounds;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "scraped_at")
    private LocalDateTime scrapedAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }

    public String getInterviewRounds() { return interviewRounds; }
    public void setInterviewRounds(String interviewRounds) { this.interviewRounds = interviewRounds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getScrapedAt() { return scrapedAt; }
    public void setScrapedAt(LocalDateTime scrapedAt) { this.scrapedAt = scrapedAt; }
}
