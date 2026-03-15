package com.plum.endorsements.infrastructure.persistence.repository;

import com.plum.endorsements.infrastructure.persistence.entity.ErrorResolutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataErrorResolutionRepository extends JpaRepository<ErrorResolutionEntity, UUID> {
    List<ErrorResolutionEntity> findByEndorsementId(UUID endorsementId);
    long countByAutoApplied(boolean autoApplied);
    List<ErrorResolutionEntity> findByEndorsementIdAndOutcomeIsNull(UUID endorsementId);
    long countByOutcome(String outcome);
}
