package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.Endorsement;

import java.util.List;

public interface AnomalyDetectionPort {

    AnomalyResult analyzeEndorsement(Endorsement endorsement, List<Endorsement> recentHistory);

    record AnomalyResult(String anomalyType, double score, String explanation) {
        public boolean isFlagged(double threshold) {
            return score >= threshold;
        }
    }
}
