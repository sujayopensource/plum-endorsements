package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "balance_forecasts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BalanceForecastEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employer_id", nullable = false)
    private UUID employerId;

    @Column(name = "insurer_id", nullable = false)
    private UUID insurerId;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "forecasted_amount", nullable = false)
    private BigDecimal forecastedAmount;

    @Column(name = "actual_amount")
    private BigDecimal actualAmount;

    @Column
    private BigDecimal accuracy;

    @Column(columnDefinition = "TEXT")
    private String narrative;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
