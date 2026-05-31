package com.hireloop.service;

import com.hireloop.model.PrepReadiness;
import com.hireloop.repository.PrepReadinessRepository;
import com.hireloop.repository.TopicCoverageRepository;
import com.hireloop.repository.TopicUniverseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrepTrackerServiceTest {

    @Mock
    private PrepReadinessRepository prepReadinessRepository;

    @Mock
    private TopicCoverageRepository topicCoverageRepository;

    @Mock
    private TopicUniverseRepository topicUniverseRepository;

    @InjectMocks
    private PrepTrackerService prepTrackerService;

    @Test
    void testUpdateCompanyReadiness() {
        // Arrange
        String companyName = "Google";
        BigDecimal dsa = new BigDecimal("80");
        BigDecimal systemDesign = new BigDecimal("85");
        BigDecimal behavioral = new BigDecimal("90");

        PrepReadiness expected = new PrepReadiness();
        expected.setCompanyName(companyName);
        expected.setDsaScore(dsa);
        expected.setSystemDesignScore(systemDesign);
        expected.setBehavioralScore(behavioral);

        when(prepReadinessRepository.findByCompanyName(companyName))
                .thenReturn(Optional.empty());
        when(prepReadinessRepository.save(any(PrepReadiness.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PrepReadiness result = prepTrackerService.updateCompanyReadiness(
                companyName, dsa, systemDesign, behavioral);

        // Assert
        assertNotNull(result);
        assertEquals(companyName, result.getCompanyName());
        assertEquals(dsa, result.getDsaScore());
        assertEquals(systemDesign, result.getSystemDesignScore());
        assertEquals(behavioral, result.getBehavioralScore());
        // Overall = dsa * 0.4 + systemDesign * 0.4 + behavioral * 0.2
        // = 80 * 0.4 + 85 * 0.4 + 90 * 0.2 = 32 + 34 + 18 = 84.0
        BigDecimal expectedOverall = new BigDecimal("84.0");
        assertEquals(expectedOverall, result.getOverallScore());
    }

    @Test
    void testCalculateGlobalReadiness_NoTopics() {
        // Arrange
        when(topicCoverageRepository.findAll()).thenReturn(java.util.List.of());

        // Act
        BigDecimal result = prepTrackerService.calculateGlobalReadiness();

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }
}
