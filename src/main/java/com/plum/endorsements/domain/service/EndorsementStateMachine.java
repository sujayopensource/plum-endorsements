package com.plum.endorsements.domain.service;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementStatus;
import org.springframework.stereotype.Component;

@Component
public class EndorsementStateMachine {

    public void transition(Endorsement endorsement, EndorsementStatus targetStatus) {
        EndorsementStatus current = endorsement.getStatus();
        if (!current.canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                "Invalid state transition: %s -> %s for endorsement %s"
                    .formatted(current, targetStatus, endorsement.getId()));
        }
        endorsement.transitionTo(targetStatus);
    }

    public boolean canTransition(Endorsement endorsement, EndorsementStatus targetStatus) {
        return endorsement.getStatus().canTransitionTo(targetStatus);
    }
}
