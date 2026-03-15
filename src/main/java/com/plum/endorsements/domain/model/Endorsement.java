package com.plum.endorsements.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Endorsement {

    private UUID id;
    private UUID employerId;
    private UUID employeeId;
    private UUID insurerId;
    private UUID policyId;
    private EndorsementType type;
    private EndorsementStatus status;
    private LocalDate coverageStartDate;
    private LocalDate coverageEndDate;
    private JsonNode employeeData;
    private BigDecimal premiumAmount;
    private UUID batchId;
    private String insurerReference;
    private int retryCount;
    private String failureReason;
    private String idempotencyKey;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    /**
     * Transitions the endorsement to the given status, validating that the
     * transition is allowed from the current status. Updates the {@code updatedAt}
     * timestamp on success.
     *
     * @param newStatus the target status
     * @throws IllegalStateException if the transition is not allowed
     */
    public void transitionTo(EndorsementStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from " + status + " to " + newStatus);
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Returns {@code true} if this endorsement can be retried — i.e. fewer than
     * 3 retries have been attempted and the current status is REJECTED.
     */
    public boolean canRetry() {
        return retryCount < 3 && status == EndorsementStatus.REJECTED;
    }

    /**
     * Delegates to the current status to determine whether this endorsement
     * has reached a terminal state.
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }

    /**
     * Increments the retry counter and moves the endorsement into the
     * RETRY_PENDING state.
     */
    public void incrementRetry() {
        this.retryCount++;
        this.status = EndorsementStatus.RETRY_PENDING;
        this.updatedAt = Instant.now();
    }
}
