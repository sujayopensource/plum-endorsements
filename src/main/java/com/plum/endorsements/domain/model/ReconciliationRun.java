package com.plum.endorsements.domain.model;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRun {
    private UUID id;
    private UUID insurerId;
    @Builder.Default
    private String status = "RUNNING";
    @Builder.Default
    private int totalChecked = 0;
    @Builder.Default
    private int matched = 0;
    @Builder.Default
    private int partialMatched = 0;
    @Builder.Default
    private int rejected = 0;
    @Builder.Default
    private int missing = 0;
    private Instant startedAt;
    private Instant completedAt;

    public void incrementMatched() { matched++; totalChecked++; }
    public void incrementPartialMatched() { partialMatched++; totalChecked++; }
    public void incrementRejected() { rejected++; totalChecked++; }
    public void incrementMissing() { missing++; totalChecked++; }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
    }
}
