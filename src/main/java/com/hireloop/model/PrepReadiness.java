package com.hireloop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prep_readiness", uniqueConstraints = {
    @UniqueConstraint(columnNames = "company_name")
})
public class PrepReadiness {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String companyName;
    private BigDecimal dsaScore = BigDecimal.ZERO;
    private BigDecimal systemDesignScore = BigDecimal.ZERO;
    private BigDecimal behavioralScore = BigDecimal.ZERO;
    private BigDecimal overallScore = BigDecimal.ZERO;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public BigDecimal getDsaScore() { return dsaScore; }
    public void setDsaScore(BigDecimal dsaScore) { this.dsaScore = dsaScore; }

    public BigDecimal getSystemDesignScore() { return systemDesignScore; }
    public void setSystemDesignScore(BigDecimal systemDesignScore) { this.systemDesignScore = systemDesignScore; }

    public BigDecimal getBehavioralScore() { return behavioralScore; }
    public void setBehavioralScore(BigDecimal behavioralScore) { this.behavioralScore = behavioralScore; }

    public BigDecimal getOverallScore() { return overallScore; }
    public void setOverallScore(BigDecimal overallScore) { this.overallScore = overallScore; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
