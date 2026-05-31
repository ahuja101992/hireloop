package com.hireloop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_topic_frequency", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"company_name", "topic_id"})
})
public class CompanyTopicFrequency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String companyName;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private TopicUniverse topic;

    private BigDecimal frequency = BigDecimal.ZERO;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public TopicUniverse getTopic() { return topic; }
    public void setTopic(TopicUniverse topic) { this.topic = topic; }

    public BigDecimal getFrequency() { return frequency; }
    public void setFrequency(BigDecimal frequency) { this.frequency = frequency; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
