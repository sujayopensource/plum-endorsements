package com.plum.endorsements.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum EndorsementStatus {

    CREATED(EnumSet.of(LazyRef.VALIDATED)),
    VALIDATED(EnumSet.of(LazyRef.PROVISIONALLY_COVERED)),
    PROVISIONALLY_COVERED(EnumSet.of(LazyRef.SUBMITTED_REALTIME, LazyRef.QUEUED_FOR_BATCH)),
    SUBMITTED_REALTIME(EnumSet.of(LazyRef.INSURER_PROCESSING, LazyRef.REJECTED)),
    QUEUED_FOR_BATCH(EnumSet.of(LazyRef.BATCH_SUBMITTED)),
    BATCH_SUBMITTED(EnumSet.of(LazyRef.INSURER_PROCESSING, LazyRef.REJECTED)),
    INSURER_PROCESSING(EnumSet.of(LazyRef.CONFIRMED, LazyRef.REJECTED)),
    CONFIRMED(EnumSet.noneOf(LazyRef.class)),
    REJECTED(EnumSet.of(LazyRef.RETRY_PENDING, LazyRef.FAILED_PERMANENT)),
    RETRY_PENDING(EnumSet.of(LazyRef.SUBMITTED_REALTIME, LazyRef.QUEUED_FOR_BATCH, LazyRef.FAILED_PERMANENT)),
    FAILED_PERMANENT(EnumSet.noneOf(LazyRef.class));

    private final Set<LazyRef> allowedTransitions;

    EndorsementStatus(Set<LazyRef> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    public boolean canTransitionTo(EndorsementStatus next) {
        return allowedTransitions.contains(LazyRef.valueOf(next.name()));
    }

    public boolean isTerminal() {
        return allowedTransitions.isEmpty();
    }

    public boolean isActive() {
        return !isTerminal();
    }

    public boolean requiresInsurerAction() {
        return this == SUBMITTED_REALTIME || this == BATCH_SUBMITTED || this == INSURER_PROCESSING;
    }

    /**
     * Internal enum used to break the forward-reference cycle that would occur
     * if EndorsementStatus constants referenced other EndorsementStatus constants
     * in their own constructor arguments. The names mirror EndorsementStatus exactly.
     */
    private enum LazyRef {
        CREATED, VALIDATED, PROVISIONALLY_COVERED,
        SUBMITTED_REALTIME, QUEUED_FOR_BATCH, BATCH_SUBMITTED,
        INSURER_PROCESSING, CONFIRMED, REJECTED,
        RETRY_PENDING, FAILED_PERMANENT
    }
}
