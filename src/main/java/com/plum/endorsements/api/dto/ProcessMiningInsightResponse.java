package com.plum.endorsements.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ProcessMiningInsightResponse(
        UUID insurerId,
        String insurerName,
        String insightType,
        String insight,
        Instant calculatedAt
) {}
