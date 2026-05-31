package com.hireloop.repository;

import com.hireloop.model.TopicUniverse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopicUniverseRepository extends JpaRepository<TopicUniverse, Integer> {
    Optional<TopicUniverse> findByCategoryAndTopic(String category, String topic);
}
