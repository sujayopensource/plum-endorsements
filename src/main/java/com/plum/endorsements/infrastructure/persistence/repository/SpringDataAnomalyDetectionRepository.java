package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.AnomalyDetectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SpringDataAnomalyDetectionRepository extends JpaRepository<AnomalyDetectionEntity, UUID> {
    List<AnomalyDetectionEntity> findByEmployerId(UUID employerId);
    List<AnomalyDetectionEntity> findByStatus(String status);
    List<AnomalyDetectionEntity> findByEmployerIdAndFlaggedAtAfter(UUID employerId, Instant after);
    long countByEmployerIdAndFlaggedAtAfter(UUID employerId, Instant after);
    long countByAnomalyTypeAndStatus(String anomalyType, String status);
    long countByAnomalyType(String anomalyType);
}
