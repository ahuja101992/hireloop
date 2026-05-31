package com.hireloop.repository;

import com.hireloop.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Integer> {
    List<Job> findByCompanyName(String companyName);
    List<Job> findByStatus(String status);
    List<Job> findByCompanyNameAndTitle(String companyName, String title);
}
