package com.hireloop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "topic_universe", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"category", "topic"})
})
public class TopicUniverse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String category;
    private String topic;
    private BigDecimal globalFrequency;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TopicUniverse() {}

    public TopicUniverse(String category, String topic) {
        this.category = category;
        this.topic = topic;
        this.globalFrequency = BigDecimal.ZERO;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public BigDecimal getGlobalFrequency() { return globalFrequency; }
    public void setGlobalFrequency(BigDecimal globalFrequency) { this.globalFrequency = globalFrequency; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
