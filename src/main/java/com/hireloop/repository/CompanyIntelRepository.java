package com.hireloop.repository;

import com.hireloop.model.CompanyIntel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyIntelRepository extends JpaRepository<CompanyIntel, Integer> {
    List<CompanyIntel> findByCompanyName(String companyName);
    Optional<CompanyIntel> findByCompanyNameAndSource(String companyName, String source);
}
