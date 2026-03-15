package com.plum.endorsements.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateEndorsementRequest(
        @NotNull UUID employerId,
        @NotNull UUID employeeId,
        @NotNull UUID insurerId,
        @NotNull UUID policyId,
        @NotBlank String type,
        @NotNull LocalDate coverageStartDate,
        LocalDate coverageEndDate,
        @NotNull JsonNode employeeData,
        BigDecimal premiumAmount,
        String idempotencyKey
) {
}
