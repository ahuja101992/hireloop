package com.hireloop.repository;

import com.hireloop.model.ResumeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeMasterRepository extends JpaRepository<ResumeMaster, Integer> {
    Optional<ResumeMaster> findByUserId(Integer userId);
    List<ResumeMaster> findByUserIdOrderByUpdatedAtDesc(Integer userId);
}
