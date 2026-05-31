package com.hireloop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireloop.config.AppConfig;
import com.hireloop.model.CompanyIntel;
import com.hireloop.model.CompanyTopicFrequency;
import com.hireloop.model.TopicUniverse;
import com.hireloop.provider.LlmProvider;
import com.hireloop.provider.LlmProviderFactory;
import com.hireloop.repository.CompanyIntelRepository;
import com.hireloop.repository.CompanyTopicFrequencyRepository;
import com.hireloop.repository.TopicUniverseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IntelScrapeService {
    private static final Logger log = LoggerFactory.getLogger(IntelScrapeService.class);

    private final CompanyIntelRepository companyIntelRepository;
    private final TopicUniverseRepository topicUniverseRepository;
    private final CompanyTopicFrequencyRepository companyTopicFrequencyRepository;
    private final LlmProviderFactory llmProviderFactory;
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntelScrapeService(
            CompanyIntelRepository companyIntelRepository,
            TopicUniverseRepository topicUniverseRepository,
            CompanyTopicFrequencyRepository companyTopicFrequencyRepository,
            LlmProviderFactory llmProviderFactory,
            AppConfig appConfig) {
        this.companyIntelRepository = companyIntelRepository;
        this.topicUniverseRepository = topicUniverseRepository;
        this.companyTopicFrequencyRepository = companyTopicFrequencyRepository;
        this.llmProviderFactory = llmProviderFactory;
        this.appConfig = appConfig;
    }

    public void scrapeAllIntel() {
        log.info("Starting intel scrape for all companies");
        try {
            var targetConfig = appConfig.getTargets();
            if (targetConfig != null && targetConfig.getCompanies() != null) {
                for (var company : targetConfig.getCompanies()) {
                    try {
                        log.debug("Scraping intel for company: {}", company.getName());
                        String mockRawData = generateMockInterviewData(company.getName());
                        scrapeAndStoreIntel(company.getName(), "aggregated", mockRawData);
                    } catch (Exception e) {
                        log.warn("Failed to scrape intel for {}: {}", company.getName(), e.getMessage());
                    }
                }
            }
            log.info("Intel scrape completed");
        } catch (Exception e) {
            log.error("Error during intel scrape", e);
        }
    }

    public void scrapeAndStoreIntel(String companyName, String source, String rawData) {
        try {
            LlmProvider provider = llmProviderFactory.getProvider();
            JsonNode intelJson = provider.extractIntel(rawData);

            // Store raw intel
            CompanyIntel intel = new CompanyIntel();
            intel.setCompanyName(companyName);
            intel.setSource(source);
            intel.setRawData(rawData);
            intel.setInterviewRounds(intelJson.toString());
            intel.setScrapedAt(LocalDateTime.now());
            companyIntelRepository.save(intel);

            // Extract and store topic frequencies
            if (intelJson.has("topic_frequencies")) {
                JsonNode topicFrequencies = intelJson.get("topic_frequencies");
                for (JsonNode topicFreq : topicFrequencies) {
                    String topicName = topicFreq.has("topic") ? topicFreq.get("topic").asText() : "unknown";
                    String category = topicFreq.has("category") ? topicFreq.get("category").asText() : "DSA";
                    double frequency = topicFreq.has("frequency") ? topicFreq.get("frequency").asDouble() : 0.5;

                    TopicUniverse topicEntity = topicUniverseRepository
                            .findByCategoryAndTopic(category, topicName)
                            .orElseGet(() -> {
                                TopicUniverse newTopic = new TopicUniverse(category, topicName);
                                newTopic.setGlobalFrequency(new BigDecimal(String.valueOf(frequency)));
                                newTopic.setCreatedAt(LocalDateTime.now());
                                newTopic.setUpdatedAt(LocalDateTime.now());
                                return topicUniverseRepository.save(newTopic);
                            });

                    // Update or create company-topic frequency
                    Optional<CompanyTopicFrequency> existing = companyTopicFrequencyRepository
                            .findByCompanyNameAndTopicId(companyName, Math.toIntExact(topicEntity.getId()));

                    CompanyTopicFrequency freq = existing.orElseGet(CompanyTopicFrequency::new);
                    freq.setCompanyName(companyName);
                    freq.setTopic(topicEntity);
                    freq.setFrequency(new BigDecimal(String.valueOf(frequency)));
                    companyTopicFrequencyRepository.save(freq);
                }
            }

            log.info("Stored intel for company: {} from source: {}", companyName, source);
        } catch (Exception e) {
            log.error("Error scraping intel for " + companyName, e);
            throw new RuntimeException("Error scraping intel for " + companyName, e);
        }
    }

    private String generateMockInterviewData(String companyName) {
        return String.format("""
            Interview Process for %s Principal SWE:

            Round 1: Coding Interview (90 minutes)
            - Focus: Dynamic Programming, Graphs, Trees
            - Difficulty: Medium to Hard

            Round 2: System Design (90 minutes)
            - Focus: Distributed Systems, Scalability, Cache
            - Difficulty: Hard

            Round 3: Behavioral (45 minutes)
            - Focus: Leadership, Technical communication
            - Difficulty: Medium

            Common topics:
            - Distributed consensus algorithms
            - Payment systems
            - Rate limiting
            - Cache invalidation
            """, companyName);
    }
}
