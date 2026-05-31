package com.hireloop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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

    @Bean
    public TargetsConfig targetsConfig() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            ClassPathResource resource = new ClassPathResource("config/targets.yml");
            TargetsConfig config = mapper.readValue(resource.getInputStream(), TargetsConfig.class);
            logger.info("Loaded {} target companies from targets.yml", config.getCompanies().size());
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
    public static class TargetsConfig {
        private List<Company> companies = new ArrayList<>();

        public List<Company> getCompanies() {
            return companies;
        }

        @Data
        public static class Company {
            private String name;
            private String ats;
            private String careers_url;
            private String api_url;
            private String priority;
            private Integer apply_readiness_threshold;

            // Getters for camelCase access
            public String getName() {
                return name;
            }

            public String getAts() {
                return ats;
            }

            public String getCareersUrl() {
                return careers_url;
            }

            public String getApiUrl() {
                return api_url;
            }

            public String getPriority() {
                return priority;
            }

            public Integer getApplyReadinessThreshold() {
                return apply_readiness_threshold;
            }
        }
    }

    @Data
    public static class FiltersConfig {
        private Integer max_age_days;
        private Integer min_fit_score;
        private Boolean require_direct_apply;
        private List<String> target_levels = new ArrayList<>();
        private List<String> locations = new ArrayList<>();
        private Integer salary_min;
        private List<String> exclude_keywords = new ArrayList<>();
        private List<String> require_keywords = new ArrayList<>();
        private Integer apply_readiness_threshold_default;

        // Getters for camelCase names for consistency with service code
        public Integer getMaxAgeDays() {
            return max_age_days;
        }

        public Integer getMinFitScore() {
            return min_fit_score;
        }

        public Boolean getRequireDirectApply() {
            return require_direct_apply;
        }

        public List<String> getTargetLevels() {
            return target_levels;
        }

        public List<String> getLocations() {
            return locations;
        }

        public Integer getSalaryMin() {
            return salary_min;
        }

        public List<String> getExcludeKeywords() {
            return exclude_keywords;
        }

        public List<String> getRequireKeywords() {
            return require_keywords;
        }

        public Integer getApplyReadinessThresholdDefault() {
            return apply_readiness_threshold_default;
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
