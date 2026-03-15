package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.AnomalyDetection;
import com.plum.endorsements.domain.model.AnomalyStatus;
import com.plum.endorsements.domain.model.AnomalyType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnomalyDetectionRepository {
    AnomalyDetection save(AnomalyDetection anomaly);
    Optional<AnomalyDetection> findById(UUID id);
    List<AnomalyDetection> findByEmployerId(UUID employerId);
    List<AnomalyDetection> findByStatus(AnomalyStatus status);
    List<AnomalyDetection> findByEmployerIdAndFlaggedAtAfter(UUID employerId, Instant after);
    long countByEmployerIdAndFlaggedAtAfter(UUID employerId, Instant after);
    long countByAnomalyTypeAndStatus(AnomalyType anomalyType, AnomalyStatus status);
    long countByAnomalyType(AnomalyType anomalyType);
}
