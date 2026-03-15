package com.plum.endorsements.api.dto;

import com.plum.endorsements.domain.model.ErrorResolution;

import java.time.Instant;
import java.util.UUID;

public record ErrorResolutionResponse(
        UUID id,
        UUID endorsementId,
        String errorType,
        String originalValue,
        String correctedValue,
        String resolution,
        double confidence,
        boolean autoApplied,
        Instant createdAt
) {
    public static ErrorResolutionResponse from(ErrorResolution er) {
        return new ErrorResolutionResponse(
                er.getId(),
                er.getEndorsementId(),
                er.getErrorType(),
                er.getOriginalValue(),
                er.getCorrectedValue(),
                er.getResolution(),
                er.getConfidence(),
                er.isAutoApplied(),
                er.getCreatedAt()
        );
    }
}
