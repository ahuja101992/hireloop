package com.hireloop.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClaudeProvider implements LlmProvider {
    private final String apiKey;
    private final String model;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";

    public ClaudeProvider(
            @Value("${llm.claude.api-key}") String apiKey,
            @Value("${llm.claude.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public JsonNode scoreJob(String resume, String jobDescription) {
        String prompt = Prompts.scoreJobPrompt(resume, jobDescription);
        return callClaude(prompt);
    }

    @Override
    public JsonNode adaptResume(String resume, String jobDescription) {
        String prompt = Prompts.adaptResumePrompt(resume, jobDescription);
        return callClaude(prompt);
    }

    @Override
    public JsonNode extractIntel(String rawText) {
        String prompt = Prompts.extractIntelPrompt(rawText);
        return callClaude(prompt);
    }

    @Override
    public JsonNode generateBrief(String companyName, String topicsJson) {
        String prompt = Prompts.generateBriefPrompt(companyName, topicsJson);
        return callClaude(prompt);
    }

    private String extractJsonFromResponse(String text) {
        String jsonText = text.trim();

        // Try to find JSON within markdown code blocks
        if (jsonText.contains("```json")) {
            int startIdx = jsonText.indexOf("```json") + 7;
            int endIdx = jsonText.indexOf("```", startIdx);
            if (endIdx > startIdx) {
                return jsonText.substring(startIdx, endIdx).trim();
            }
        } else if (jsonText.contains("```")) {
            int startIdx = jsonText.indexOf("```") + 3;
            int endIdx = jsonText.indexOf("```", startIdx);
            if (endIdx > startIdx) {
                return jsonText.substring(startIdx, endIdx).trim();
            }
        }

        // If no code blocks found, try to find JSON object directly
        // Look for the first { and last } to extract potential JSON
        int startIdx = jsonText.indexOf('{');
        if (startIdx >= 0) {
            int endIdx = jsonText.lastIndexOf('}');
            if (endIdx > startIdx) {
                return jsonText.substring(startIdx, endIdx + 1).trim();
            }
        }

        // If no JSON found, return original text (will fail parsing with proper error)
        return jsonText;
    }

    private JsonNode callClaude(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", 4096);
            body.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));

            HttpEntity<String> request = new HttpEntity<>(
                    objectMapper.writeValueAsString(body),
                    headers
            );

            String response = restTemplate.postForObject(CLAUDE_API_URL, request, String.class);
            JsonNode responseNode = objectMapper.readTree(response);

            // Extract text from Claude's response format
            String text = responseNode.get("content").get(0).get("text").asText();

            // Handle markdown-wrapped JSON responses and extract JSON
            String jsonText = extractJsonFromResponse(text);

            // Parse the JSON from the response
            return objectMapper.readTree(jsonText);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing JSON response from Claude", e);
        } catch (Exception e) {
            throw new RuntimeException("Error calling Claude API", e);
        }
    }
}
