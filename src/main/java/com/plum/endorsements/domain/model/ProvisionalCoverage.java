package com.plum.endorsements.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionalCoverage {

    private UUID id;
    private UUID endorsementId;
    private UUID employeeId;
    private UUID employerId;
    private LocalDate coverageStart;
    @Builder.Default
    private String coverageType = "PROVISIONAL";
    private Instant confirmedAt;
    private Instant expiredAt;
    private Instant createdAt;

    /**
     * Returns {@code true} if this coverage is still active — i.e. it has
     * been neither confirmed nor expired.
     */
    public boolean isActive() {
        return confirmedAt == null && expiredAt == null;
    }

    /**
     * Confirms the provisional coverage at the given instant and upgrades
     * the coverage type to CONFIRMED.
     */
    public void confirm(Instant at) {
        this.confirmedAt = at;
        this.coverageType = "CONFIRMED";
    }

    /**
     * Marks the provisional coverage as expired at the given instant.
     */
    public void expire(Instant at) {
        this.expiredAt = at;
    }
}
