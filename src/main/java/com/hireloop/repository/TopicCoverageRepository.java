package com.hireloop.repository;

import com.hireloop.model.TopicCoverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopicCoverageRepository extends JpaRepository<TopicCoverage, Integer> {
    Optional<TopicCoverage> findByTopicId(Integer topicId);
}
