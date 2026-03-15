package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "endorsements")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EndorsementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employer_id", nullable = false)
    private UUID employerId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "insurer_id", nullable = false)
    private UUID insurerId;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "coverage_start_date", nullable = false)
    private LocalDate coverageStartDate;

    @Column(name = "coverage_end_date")
    private LocalDate coverageEndDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "employee_data", nullable = false, columnDefinition = "jsonb")
    private String employeeData;

    @Column(name = "premium_amount")
    private BigDecimal premiumAmount;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "insurer_reference", length = 100)
    private String insurerReference;

    @Column(name = "retry_count")
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Version
    private int version;
}
