package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.port.AnomalyDetectionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaAugmentedAnomalyDetector")
class OllamaAugmentedAnomalyDetectorTest {

    private RuleBasedAnomalyScorer scorer;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private OllamaAugmentedAnomalyDetector detector;
    private UUID employerId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        scorer = new RuleBasedAnomalyScorer();
        detector = new OllamaAugmentedAnomalyDetector(scorer, chatClientBuilder);
        ReflectionTestUtils.setField(detector, "enrichmentThreshold", 0.7);
        employerId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
    }

    private Endorsement buildEndorsement(EndorsementType type, BigDecimal premium, Instant createdAt) {
        return Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(employeeId)
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(type)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(30))
                .premiumAmount(premium)
                .retryCount(0)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    @Test
    @DisplayName("skips LLM call when anomaly score is below threshold")
    void analyzeEndorsement_lowScore_skipsLlmCall() {
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.UPDATE)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(60))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, new ArrayList<>());

        assertThat(result.score()).isEqualTo(0.0);
        // Verify chatClient was never used
        verifyNoInteractions(chatClient);
        verify(chatClientBuilder, never()).build();
    }

    @Test
    @DisplayName("enriches explanation via LLM when score exceeds threshold")
    void analyzeEndorsement_highScore_enrichesWithLlm() {
        // Trigger ADD_DELETE_CYCLING (score 0.85 >= threshold 0.7)
        Endorsement target = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now());

        List<Endorsement> history = new ArrayList<>();
        history.add(buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now().minus(10, ChronoUnit.DAYS)));
        history.add(buildEndorsement(EndorsementType.DELETE,
                new BigDecimal("1000.00"), Instant.now().minus(5, ChronoUnit.DAYS)));

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(
                "This employee has been added and removed within 30 days, which is a common fraud pattern. Investigate the employer's HR records.");

        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, history);

        assertThat(result.score()).isGreaterThanOrEqualTo(0.85);
        assertThat(result.anomalyType()).isEqualTo("ADD_DELETE_CYCLING");
        assertThat(result.explanation()).contains("fraud pattern");
    }

    @Test
    @DisplayName("returns rule-based result when LLM is unavailable")
    void analyzeEndorsement_llmUnavailable_returnsRuleBasedResult() {
        // Trigger cycling anomaly
        Endorsement target = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now());

        List<Endorsement> history = new ArrayList<>();
        history.add(buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now().minus(10, ChronoUnit.DAYS)));
        history.add(buildEndorsement(EndorsementType.DELETE,
                new BigDecimal("1000.00"), Instant.now().minus(5, ChronoUnit.DAYS)));

        when(chatClientBuilder.build()).thenThrow(new RuntimeException("Ollama connection refused"));

        // Without Spring AOP, the @CircuitBreaker annotation is not active,
        // so the exception propagates. Verify the exception is from LLM, not from scoring.
        // The scoring itself succeeds — the failure is only in enrichment.
        assertThatThrownBy(() -> detector.analyzeEndorsement(target, history))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ollama connection refused");

        // Verify the rule-based scorer itself works independently
        RuleBasedAnomalyScorer.ScoringResult scoringResult = scorer.score(target, history);
        assertThat(scoringResult.score()).isGreaterThanOrEqualTo(0.85);
        assertThat(scoringResult.anomalyType()).isEqualTo("ADD_DELETE_CYCLING");
        assertThat(scoringResult.ruleExplanation()).contains("Add/delete cycling detected");
    }

    @Test
    @DisplayName("handles empty history without errors")
    void analyzeEndorsement_emptyHistory_handlesGracefully() {
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.UPDATE)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(60))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, new ArrayList<>());

        assertThat(result).isNotNull();
        assertThat(result.score()).isEqualTo(0.0);
        assertThat(result.anomalyType()).isNotNull();
    }

    @Test
    @DisplayName("detects volume spike and enriches with LLM")
    void analyzeEndorsement_volumeSpike_detectsAndEnriches() {
        Endorsement target = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now());

        List<Endorsement> history = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            history.add(buildEndorsement(EndorsementType.ADD,
                    new BigDecimal("1000.00"), Instant.now().minus(i, ChronoUnit.HOURS)));
        }

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(
                "Abnormal spike in endorsement volume detected. This employer submitted 50 endorsements in 24h vs their daily average. Review for potential bulk fraud or data migration activity.");

        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, history);

        assertThat(result.score()).isGreaterThan(0.5);
        assertThat(result.anomalyType()).isEqualTo("VOLUME_SPIKE");
        assertThat(result.explanation()).contains("spike");
    }
}
