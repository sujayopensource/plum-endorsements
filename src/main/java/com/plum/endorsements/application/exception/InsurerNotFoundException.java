package com.plum.endorsements.application.exception;

import java.util.UUID;

public class InsurerNotFoundException extends RuntimeException {

    public InsurerNotFoundException(UUID insurerId) {
        super("Insurer not found with ID: " + insurerId);
    }

    public InsurerNotFoundException(String insurerCode) {
        super("Insurer not found with code: " + insurerCode);
    }
}
