package com.hireloop.repository;

import com.hireloop.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    List<Application> findByJobId(Integer jobId);
    List<Application> findByPipelineStatus(String pipelineStatus);
}
