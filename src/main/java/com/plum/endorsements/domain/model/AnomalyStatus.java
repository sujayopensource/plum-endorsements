package com.plum.endorsements.domain.model;

public enum AnomalyStatus {
    FLAGGED,
    UNDER_REVIEW,
    DISMISSED,
    CONFIRMED_FRAUD;

    public boolean canTransitionTo(AnomalyStatus target) {
        return switch (this) {
            case FLAGGED -> target == UNDER_REVIEW || target == DISMISSED;
            case UNDER_REVIEW -> target == DISMISSED || target == CONFIRMED_FRAUD;
            case DISMISSED, CONFIRMED_FRAUD -> false;
        };
    }

    public boolean isTerminal() {
        return this == DISMISSED || this == CONFIRMED_FRAUD;
    }
}
