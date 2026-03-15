package com.plum.endorsements.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AnomalyReviewRequest(
        @NotBlank String status,
        String notes
) {}
