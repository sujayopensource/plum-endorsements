package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.port.AnomalyDetectionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "endorsement.intelligence.ollama.enabled", havingValue = "false", matchIfMissing = true)
public class RuleBasedAnomalyDetector implements AnomalyDetectionPort {

    private final RuleBasedAnomalyScorer scorer;

    @Override
    public AnomalyResult analyzeEndorsement(Endorsement endorsement, List<Endorsement> recentHistory) {
        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(endorsement, recentHistory);

        log.debug("Anomaly analysis for endorsement {}: type={}, score={:.4f}",
                endorsement.getId(), result.anomalyType(), result.score());

        return new AnomalyResult(result.anomalyType(), result.score(), result.ruleExplanation());
    }
}
