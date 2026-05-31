package com.hireloop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private static TargetsConfig targets;

    public TargetsConfig getTargets() {
        return targets;
    }

    @Bean
    public TargetsConfig targetsConfig() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            ClassPathResource resource = new ClassPathResource("config/targets.yml");
            TargetsConfig config = mapper.readValue(resource.getInputStream(), TargetsConfig.class);
            logger.info("Loaded {} target companies from targets.yml", config.getCompanies().size());
            targets = config;
            return config;
        } catch (IOException e) {
            logger.error("Error loading targets.yml", e);
            throw new RuntimeException("Failed to load targets.yml", e);
        }
    }

    @Bean
    public FiltersConfig filtersConfig() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            ClassPathResource resource = new ClassPathResource("config/filters.yml");
            FiltersConfigWrapper wrapper = mapper.readValue(resource.getInputStream(), FiltersConfigWrapper.class);
            FiltersConfig config = wrapper.getFilters();
            logger.info("Loaded job filters from filters.yml: max_age_days={}, min_fit_score={}",
                config.getMaxAgeDays(), config.getMinFitScore());
            return config;
        } catch (IOException e) {
            logger.error("Error loading filters.yml", e);
            throw new RuntimeException("Failed to load filters.yml", e);
        }
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class TargetsConfig {
        private List<Company> companies = new ArrayList<>();
        private List<String> intelSources = new ArrayList<>();

        public List<Company> getCompanies() {
            return companies;
        }

        public List<String> getIntelSources() {
            return intelSources;
        }

        @Data
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Company {
            private String name;
            private String ats;
            private String careersUrl;
            private String apiUrl;
            private String priority;
            private Integer applyReadinessThreshold;

            public String getName() {
                return name;
            }

            public String getAts() {
                return ats;
            }

            public String getCareersUrl() {
                return careersUrl;
            }

            public String getApiUrl() {
                return apiUrl;
            }

            public String getPriority() {
                return priority;
            }

            public Integer getApplyReadinessThreshold() {
                return applyReadinessThreshold;
            }
        }
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class FiltersConfig {
        private Integer maxAgeDays;
        private Integer minFitScore;
        private Boolean requireDirectApply;
        private List<String> targetLevels = new ArrayList<>();
        private List<String> locations = new ArrayList<>();
        private Integer salaryMin;
        private List<String> excludeKeywords = new ArrayList<>();
        private List<String> requireKeywords = new ArrayList<>();
        private Integer applyReadinessThresholdDefault;

        public Integer getMaxAgeDays() {
            return maxAgeDays;
        }

        public Integer getMinFitScore() {
            return minFitScore;
        }

        public Boolean getRequireDirectApply() {
            return requireDirectApply;
        }

        public List<String> getTargetLevels() {
            return targetLevels;
        }

        public List<String> getLocations() {
            return locations;
        }

        public Integer getSalaryMin() {
            return salaryMin;
        }

        public List<String> getExcludeKeywords() {
            return excludeKeywords;
        }

        public List<String> getRequireKeywords() {
            return requireKeywords;
        }

        public Integer getApplyReadinessThresholdDefault() {
            return applyReadinessThresholdDefault;
        }
    }

    @Data
    public static class FiltersConfigWrapper {
        private FiltersConfig filters;

        public FiltersConfig getFilters() {
            return filters;
        }
    }
}
