package com.hireloop.repository;

import com.hireloop.model.PrepReadiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrepReadinessRepository extends JpaRepository<PrepReadiness, Integer> {
    Optional<PrepReadiness> findByCompanyName(String companyName);
}
