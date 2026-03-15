package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.ErrorResolution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ErrorResolutionRepository {
    ErrorResolution save(ErrorResolution resolution);
    Optional<ErrorResolution> findById(UUID id);
    List<ErrorResolution> findByEndorsementId(UUID endorsementId);
    long countByAutoApplied(boolean autoApplied);
    long count();
    List<ErrorResolution> findByEndorsementIdAndOutcomeIsNull(UUID endorsementId);
    long countByOutcome(String outcome);
}
