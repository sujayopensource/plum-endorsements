package com.plum.endorsements.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ea_accounts")
@IdClass(EAAccountId.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EAAccountEntity {
    @Id
    @Column(name = "employer_id")
    private UUID employerId;

    @Id
    @Column(name = "insurer_id")
    private UUID insurerId;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal reserved = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
