package com.plum.endorsements.domain.port;

import com.plum.endorsements.domain.model.Endorsement;

import java.util.UUID;

public interface ErrorResolutionPort {

    ResolutionSuggestion analyzeError(Endorsement endorsement, String errorMessage, UUID insurerId);

    record ResolutionSuggestion(String errorType, String originalValue, String correctedValue,
                                 String resolution, double confidence) {}
}
