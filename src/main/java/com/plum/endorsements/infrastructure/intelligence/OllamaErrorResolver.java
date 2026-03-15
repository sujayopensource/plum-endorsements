package com.plum.endorsements.infrastructure.intelligence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.port.ErrorResolutionPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "endorsement.intelligence.ollama.enabled", havingValue = "true")
public class OllamaErrorResolver implements ErrorResolutionPort {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final SimulatedErrorResolver fallbackResolver;

    public OllamaErrorResolver(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.fallbackResolver = new SimulatedErrorResolver();
    }

    @Override
    @CircuitBreaker(name = "ollamaErrorResolution", fallbackMethod = "analyzeErrorFallback")
    @Retry(name = "ollamaErrorResolution")
    public ResolutionSuggestion analyzeError(Endorsement endorsement, String errorMessage, UUID insurerId) {
        log.info("Analyzing error via Ollama LLM for endorsement {}, insurer {}: '{}'",
                endorsement.getId(), insurerId, errorMessage);

        String prompt = buildPrompt(endorsement, errorMessage, insurerId);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return parseResponse(response, errorMessage);
    }

    private String buildPrompt(Endorsement endorsement, String errorMessage, UUID insurerId) {
        return String.format("""
                You are an insurance endorsement error resolution expert for the Indian health insurance market.
                Analyze the following endorsement error and suggest a resolution.

                Context:
                - Endorsement ID: %s
                - Employer ID: %s
                - Employee ID: %s
                - Insurer ID: %s
                - Endorsement Type: %s
                - Premium Amount: %s
                - Error Message: %s

                Respond ONLY with a JSON object (no markdown, no explanation outside JSON):
                {
                  "errorType": "one of: DATE_FORMAT, MISSING_FIELD, MEMBER_ID_FORMAT, PREMIUM_MISMATCH, UNKNOWN_ERROR",
                  "originalValue": "the problematic value extracted from the error",
                  "correctedValue": "the suggested corrected value, or null if unknown",
                  "resolution": "detailed explanation of the fix",
                  "confidence": 0.0 to 1.0
                }
                """,
                endorsement.getId(),
                endorsement.getEmployerId(),
                endorsement.getEmployeeId(),
                insurerId,
                endorsement.getType(),
                endorsement.getPremiumAmount(),
                errorMessage);
    }

    private ResolutionSuggestion parseResponse(String response, String errorMessage) {
        try {
            JsonNode json = objectMapper.readTree(response);

            String errorType = json.path("errorType").asText("UNKNOWN_ERROR");
            String originalValue = json.path("originalValue").asText(null);
            String correctedValue = json.path("correctedValue").isNull()
                    ? null : json.path("correctedValue").asText(null);
            String resolution = json.path("resolution").asText("LLM analysis complete");
            double confidence = json.path("confidence").asDouble(0.5);

            confidence = Math.max(0.0, Math.min(1.0, confidence));

            log.info("Ollama resolved error: type={}, confidence={}", errorType, confidence);
            return new ResolutionSuggestion(errorType, originalValue, correctedValue, resolution, confidence);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Ollama response as JSON, falling back to rule-based: {}", e.getMessage());
            return fallbackResolver.analyzeError(
                    Endorsement.builder().build(), errorMessage, UUID.randomUUID());
        }
    }

    @SuppressWarnings("unused")
    private ResolutionSuggestion analyzeErrorFallback(Endorsement endorsement, String errorMessage,
                                                       UUID insurerId, Throwable t) {
        log.warn("Ollama error resolution fallback triggered for endorsement {}: {}",
                endorsement.getId(), t.getMessage());
        return fallbackResolver.analyzeError(endorsement, errorMessage, insurerId);
    }
}
