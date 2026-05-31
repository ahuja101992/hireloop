package com.hireloop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hireloop.model.CompanyIntel;
import com.hireloop.model.CompanyTopicFrequency;
import com.hireloop.model.TopicUniverse;
import com.hireloop.provider.LlmProvider;
import com.hireloop.provider.LlmProviderFactory;
import com.hireloop.repository.CompanyIntelRepository;
import com.hireloop.repository.CompanyTopicFrequencyRepository;
import com.hireloop.repository.TopicUniverseRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class IntelScrapeService {
    private final CompanyIntelRepository companyIntelRepository;
    private final TopicUniverseRepository topicUniverseRepository;
    private final CompanyTopicFrequencyRepository companyTopicFrequencyRepository;
    private final LlmProviderFactory llmProviderFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntelScrapeService(
            CompanyIntelRepository companyIntelRepository,
            TopicUniverseRepository topicUniverseRepository,
            CompanyTopicFrequencyRepository companyTopicFrequencyRepository,
            LlmProviderFactory llmProviderFactory) {
        this.companyIntelRepository = companyIntelRepository;
        this.topicUniverseRepository = topicUniverseRepository;
        this.companyTopicFrequencyRepository = companyTopicFrequencyRepository;
        this.llmProviderFactory = llmProviderFactory;
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
            companyIntelRepository.save(intel);

            // Extract and store topic frequencies
            if (intelJson.has("key_topics")) {
                JsonNode keyTopics = intelJson.get("key_topics");
                for (JsonNode topic : keyTopics) {
                    String topicName = topic.asText();
                    TopicUniverse topicEntity = topicUniverseRepository
                            .findByCategoryAndTopic("interview", topicName)
                            .orElseGet(() -> {
                                TopicUniverse newTopic = new TopicUniverse("interview", topicName);
                                return topicUniverseRepository.save(newTopic);
                            });

                    // Update or create company-topic frequency
                    Optional<CompanyTopicFrequency> existing = companyTopicFrequencyRepository
                            .findByCompanyNameAndTopicId(companyName, topicEntity.getId());

                    CompanyTopicFrequency freq = existing.orElseGet(CompanyTopicFrequency::new);
                    freq.setCompanyName(companyName);
                    freq.setTopic(topicEntity);
                    freq.setFrequency(freq.getFrequency().add(BigDecimal.ONE));
                    companyTopicFrequencyRepository.save(freq);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error scraping intel for " + companyName, e);
        }
    }
}
