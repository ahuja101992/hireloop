package com.hireloop.provider;

import com.fasterxml.jackson.databind.JsonNode;

public interface LlmProvider {
    JsonNode scoreJob(String resume, String jobDescription);
    JsonNode adaptResume(String resume, String jobDescription);
    JsonNode extractIntel(String rawText);
    JsonNode generateBrief(String companyName, String topicsJson);
}
