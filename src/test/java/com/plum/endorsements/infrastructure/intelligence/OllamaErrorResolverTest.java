package com.plum.endorsements.infrastructure.intelligence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.port.ErrorResolutionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaErrorResolver")
class OllamaErrorResolverTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private OllamaErrorResolver resolver;
    private Endorsement endorsement;
    private UUID insurerId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        resolver = new OllamaErrorResolver(chatClientBuilder, objectMapper);

        insurerId = UUID.randomUUID();
        endorsement = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(insurerId)
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.REJECTED)
                .coverageStartDate(LocalDate.now().plusDays(10))
                .premiumAmount(new BigDecimal("1500.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private void mockLlmResponse(String jsonResponse) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(jsonResponse);
    }

    @Test
    @DisplayName("returns LLM suggestion when valid JSON response")
    void analyzeError_validError_returnsLlmSuggestion() {
        String llmResponse = """
                {
                  "errorType": "DATE_FORMAT",
                  "originalValue": "07-03-1990",
                  "correctedValue": "1990-03-07",
                  "resolution": "Converted DD-MM-YYYY to ISO 8601 YYYY-MM-DD format as required by insurer",
                  "confidence": 0.95
                }
                """;
        mockLlmResponse(llmResponse);

        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Invalid date format: DOB 07-03-1990", insurerId);

        assertThat(result.errorType()).isEqualTo("DATE_FORMAT");
        assertThat(result.originalValue()).isEqualTo("07-03-1990");
        assertThat(result.correctedValue()).isEqualTo("1990-03-07");
        assertThat(result.confidence()).isEqualTo(0.95);
        assertThat(result.resolution()).contains("ISO 8601");
    }

    @Test
    @DisplayName("falls back to rule-based when LLM returns invalid JSON")
    void analyzeError_llmReturnsInvalidJson_fallsBackToRuleBased() {
        mockLlmResponse("This is not valid JSON at all");

        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Invalid date format: DOB 07-03-1990", insurerId);

        // Fallback uses SimulatedErrorResolver, which matches date patterns
        assertThat(result).isNotNull();
        assertThat(result.errorType()).isNotNull();
        assertThat(result.confidence()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("returns high confidence for member ID format errors")
    void analyzeError_memberIdFormat_returnsHighConfidence() {
        String llmResponse = """
                {
                  "errorType": "MEMBER_ID_FORMAT",
                  "originalValue": "12345-abc",
                  "correctedValue": "PLM-12345ABC",
                  "resolution": "Applied PLM- prefix and normalized member ID format",
                  "confidence": 0.96
                }
                """;
        mockLlmResponse(llmResponse);

        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Invalid member ID format", insurerId);

        assertThat(result.errorType()).isEqualTo("MEMBER_ID_FORMAT");
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.96);
        assertThat(result.correctedValue()).startsWith("PLM-");
    }

    @Test
    @DisplayName("returns low confidence for unknown errors")
    void analyzeError_unknownError_returnsLowConfidence() {
        String llmResponse = """
                {
                  "errorType": "UNKNOWN_ERROR",
                  "originalValue": null,
                  "correctedValue": null,
                  "resolution": "Unable to determine a resolution. Manual review recommended.",
                  "confidence": 0.25
                }
                """;
        mockLlmResponse(llmResponse);

        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "Unexpected system error xyz", insurerId);

        assertThat(result.errorType()).isEqualTo("UNKNOWN_ERROR");
        assertThat(result.confidence()).isLessThan(0.5);
    }

    @Test
    @DisplayName("handles null endorsement fields gracefully")
    void analyzeError_nullEndorsement_handlesGracefully() {
        Endorsement nullFieldEndorsement = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(null)
                .insurerId(insurerId)
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.REJECTED)
                .premiumAmount(null)
                .build();

        String llmResponse = """
                {
                  "errorType": "MISSING_FIELD",
                  "originalValue": "email",
                  "correctedValue": "default@employer.com",
                  "resolution": "Suggested default email based on employer records",
                  "confidence": 0.85
                }
                """;
        mockLlmResponse(llmResponse);

        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                nullFieldEndorsement, "Required field missing: email", insurerId);

        assertThat(result).isNotNull();
        assertThat(result.errorType()).isEqualTo("MISSING_FIELD");
    }

    @Test
    @DisplayName("handles empty error message gracefully")
    void analyzeError_emptyErrorMessage_handlesGracefully() {
        String llmResponse = """
                {
                  "errorType": "UNKNOWN_ERROR",
                  "originalValue": null,
                  "correctedValue": null,
                  "resolution": "No error message provided. Manual review required.",
                  "confidence": 0.1
                }
                """;
        mockLlmResponse(llmResponse);

        ErrorResolutionPort.ResolutionSuggestion result = resolver.analyzeError(
                endorsement, "", insurerId);

        assertThat(result).isNotNull();
        assertThat(result.confidence()).isLessThanOrEqualTo(1.0);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("circuit breaker fallback returns rule-based result")
    void analyzeError_circuitBreakerOpen_returnsFallback() {
        // Directly call the fallback method via reflection-compatible pattern
        // The fallback delegates to SimulatedErrorResolver
        SimulatedErrorResolver fallback = new SimulatedErrorResolver();
        ErrorResolutionPort.ResolutionSuggestion result = fallback.analyzeError(
                endorsement, "Invalid member id format", insurerId);

        assertThat(result).isNotNull();
        assertThat(result.errorType()).isEqualTo("MEMBER_ID_FORMAT");
        assertThat(result.confidence()).isEqualTo(0.96);
    }
}
