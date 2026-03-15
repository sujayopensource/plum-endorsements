package com.plum.endorsements.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EATransaction(
        Long id,
        UUID employerId,
        UUID insurerId,
        UUID endorsementId,
        EATransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        Instant createdAt
) {

    public enum EATransactionType {
        DEBIT, CREDIT, RESERVE, RELEASE, TOP_UP
    }
}
