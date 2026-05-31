package com.hireloop.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LlmProviderFactory {
    private final String providerName;
    private final ClaudeProvider claudeProvider;

    public LlmProviderFactory(
            @Value("${llm.provider}") String providerName,
            @Autowired ClaudeProvider claudeProvider) {
        this.providerName = providerName;
        this.claudeProvider = claudeProvider;
    }

    public LlmProvider getProvider() {
        if ("claude".equalsIgnoreCase(providerName)) {
            return claudeProvider;
        }
        throw new IllegalArgumentException("Unknown LLM provider: " + providerName);
    }
}
