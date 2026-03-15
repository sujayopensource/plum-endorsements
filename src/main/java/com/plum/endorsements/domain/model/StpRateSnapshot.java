package com.plum.endorsements.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StpRateSnapshot {

    private UUID id;
    private UUID insurerId;
    private LocalDate snapshotDate;
    private int totalEndorsements;
    private int stpEndorsements;
    private BigDecimal stpRate;
    private Instant createdAt;
}
