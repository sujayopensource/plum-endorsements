package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "provisional_coverages")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProvisionalCoverageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "endorsement_id", nullable = false)
    private UUID endorsementId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "employer_id", nullable = false)
    private UUID employerId;

    @Column(name = "coverage_start", nullable = false)
    private LocalDate coverageStart;

    @Column(name = "coverage_type", length = 20)
    @Builder.Default
    private String coverageType = "PROVISIONAL";

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
