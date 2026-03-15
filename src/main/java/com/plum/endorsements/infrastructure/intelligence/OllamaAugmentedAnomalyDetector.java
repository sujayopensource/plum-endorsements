package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.port.AnomalyDetectionPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "endorsement.intelligence.ollama.enabled", havingValue = "true")
public class OllamaAugmentedAnomalyDetector implements AnomalyDetectionPort {

    private final RuleBasedAnomalyScorer scorer;
    private final ChatClient.Builder chatClientBuilder;

    @Value("${endorsement.intelligence.anomaly-detection.min-anomaly-score:0.7}")
    private double enrichmentThreshold;

    @Override
    public AnomalyResult analyzeEndorsement(Endorsement endorsement, List<Endorsement> recentHistory) {
        RuleBasedAnomalyScorer.ScoringResult scoringResult = scorer.score(endorsement, recentHistory);

        if (scoringResult.score() < enrichmentThreshold) {
            log.debug("Anomaly score {:.4f} below threshold {}, skipping LLM enrichment for endorsement {}",
                    scoringResult.score(), enrichmentThreshold, endorsement.getId());
            return new AnomalyResult(scoringResult.anomalyType(), scoringResult.score(),
                    scoringResult.ruleExplanation());
        }

        String enrichedExplanation = enrichWithLlm(endorsement, scoringResult);
        return new AnomalyResult(scoringResult.anomalyType(), scoringResult.score(), enrichedExplanation);
    }

    @CircuitBreaker(name = "ollamaAnomalyDetection", fallbackMethod = "enrichWithLlmFallback")
    @Retry(name = "ollamaAnomalyDetection")
    private String enrichWithLlm(Endorsement endorsement, RuleBasedAnomalyScorer.ScoringResult scoringResult) {
        log.info("Enriching anomaly explanation via Ollama for endorsement {}: type={}, score={}",
                endorsement.getId(), scoringResult.anomalyType(), scoringResult.score());

        ChatClient client = chatClientBuilder.build();

        String prompt = String.format("""
                You are a fraud analyst for an Indian health insurance platform (Plum).
                A rule-based anomaly detector has flagged the following endorsement.
                Provide a concise business-context explanation (2-3 sentences) of why this is suspicious
                and what action the operations team should take.

                Anomaly Type: %s
                Anomaly Score: %.2f
                Rule Explanation: %s

                Endorsement Details:
                - Type: %s
                - Employer ID: %s
                - Employee ID: %s
                - Premium: %s
                - Coverage Start: %s

                Respond with ONLY the enriched explanation text (no JSON, no markdown).
                """,
                scoringResult.anomalyType(),
                scoringResult.score(),
                scoringResult.ruleExplanation(),
                endorsement.getType(),
                endorsement.getEmployerId(),
                endorsement.getEmployeeId(),
                endorsement.getPremiumAmount(),
                endorsement.getCoverageStartDate());

        String enriched = client.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("Ollama enriched anomaly explanation for endorsement {}", endorsement.getId());
        return enriched != null && !enriched.isBlank() ? enriched : scoringResult.ruleExplanation();
    }

    @SuppressWarnings("unused")
    private String enrichWithLlmFallback(Endorsement endorsement,
                                          RuleBasedAnomalyScorer.ScoringResult scoringResult,
                                          Throwable t) {
        log.warn("Ollama anomaly enrichment fallback for endorsement {}: {}",
                endorsement.getId(), t.getMessage());
        return scoringResult.ruleExplanation();
    }
}
