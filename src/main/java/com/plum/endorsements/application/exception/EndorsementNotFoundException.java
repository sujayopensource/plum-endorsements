package com.plum.endorsements.application.exception;

import java.util.UUID;

public class EndorsementNotFoundException extends RuntimeException {

    public EndorsementNotFoundException(UUID endorsementId) {
        super("Endorsement not found: " + endorsementId);
    }
}
