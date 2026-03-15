package com.plum.endorsements.domain.model;

public enum EndorsementPriority {
    P0_DELETION(0),
    P1_COST_NEUTRAL(1),
    P2_ADDITION(2),
    P3_PREMIUM_UPDATE(3);

    private final int rank;

    EndorsementPriority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public static EndorsementPriority classify(Endorsement endorsement) {
        return switch (endorsement.getType()) {
            case DELETE -> P0_DELETION;
            case UPDATE -> {
                if (endorsement.getPremiumAmount() != null
                        && endorsement.getPremiumAmount().signum() == 0) {
                    yield P1_COST_NEUTRAL;
                }
                yield P3_PREMIUM_UPDATE;
            }
            case ADD -> P2_ADDITION;
        };
    }
}
