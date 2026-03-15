package com.plum.endorsements.api.dto;

import com.plum.endorsements.domain.model.EAAccount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EAAccountResponse(
        UUID employerId,
        UUID insurerId,
        BigDecimal balance,
        BigDecimal reserved,
        BigDecimal availableBalance,
        Instant updatedAt
) {

    public static EAAccountResponse from(EAAccount account) {
        return new EAAccountResponse(
                account.getEmployerId(),
                account.getInsurerId(),
                account.getBalance(),
                account.getReserved(),
                account.availableBalance(),
                account.getUpdatedAt()
        );
    }
}
