package com.plum.endorsements.application.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID employerId, BigDecimal required, BigDecimal available) {
        super("Insufficient EA balance for employer " + employerId
                + ": required=" + required + ", available=" + available);
    }
}
