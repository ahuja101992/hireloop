package com.hireloop.repository;

import com.hireloop.model.CompanyTopicFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyTopicFrequencyRepository extends JpaRepository<CompanyTopicFrequency, Integer> {
    List<CompanyTopicFrequency> findByCompanyName(String companyName);
    Optional<CompanyTopicFrequency> findByCompanyNameAndTopicId(String companyName, Integer topicId);
}
