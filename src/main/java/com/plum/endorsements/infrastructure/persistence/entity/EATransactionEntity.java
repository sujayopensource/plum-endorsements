package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ea_transactions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EATransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employer_id", nullable = false)
    private UUID employerId;

    @Column(name = "insurer_id", nullable = false)
    private UUID insurerId;

    @Column(name = "endorsement_id")
    private UUID endorsementId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    private String description;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
