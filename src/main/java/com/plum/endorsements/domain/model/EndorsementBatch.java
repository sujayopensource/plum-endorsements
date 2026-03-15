package com.plum.endorsements.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndorsementBatch {

    private UUID id;
    private UUID insurerId;
    private BatchStatus status;
    private int endorsementCount;
    private BigDecimal totalPremium;
    private Instant submittedAt;
    private Instant slaDeadline;
    private String insurerBatchRef;
    private Instant createdAt;

    /**
     * Returns {@code true} if the SLA deadline has been breached — i.e. the
     * current time is past the deadline and the batch has not yet completed.
     */
    public boolean isSlaBreached(Instant now) {
        return slaDeadline != null && now.isAfter(slaDeadline) && status != BatchStatus.COMPLETE;
    }
}
