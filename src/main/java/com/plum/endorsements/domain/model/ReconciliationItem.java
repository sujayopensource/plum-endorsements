package com.plum.endorsements.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationItem {
    private UUID id;
    private UUID runId;
    private UUID endorsementId;
    private UUID batchId;
    private UUID insurerId;
    private UUID employerId;
    private ReconciliationOutcome outcome;
    private JsonNode sentData;
    private JsonNode confirmedData;
    private JsonNode discrepancyDetails;
    private String actionTaken;
    @Builder.Default
    private Instant createdAt = Instant.now();
}
