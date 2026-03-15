package com.plum.endorsements.application.exception;

public class DuplicateEndorsementException extends RuntimeException {

    public DuplicateEndorsementException(String idempotencyKey) {
        super("Duplicate endorsement with idempotency key: " + idempotencyKey);
    }
}
