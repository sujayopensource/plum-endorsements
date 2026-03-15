package com.plum.endorsements.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EndorsementPriority")
class EndorsementPriorityTest {

    private Endorsement buildEndorsement(EndorsementType type, BigDecimal premium) {
        return Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(type)
                .premiumAmount(premium)
                .coverageStartDate(LocalDate.now().plusDays(1))
                .build();
    }

    @Test
    @DisplayName("DELETE type classified as P0_DELETION")
    void classify_DeleteType_P0() {
        Endorsement e = buildEndorsement(EndorsementType.DELETE, new BigDecimal("1000"));
        assertThat(EndorsementPriority.classify(e)).isEqualTo(EndorsementPriority.P0_DELETION);
    }

    @Test
    @DisplayName("UPDATE type with zero premium classified as P1_COST_NEUTRAL")
    void classify_UpdateZeroPremium_P1() {
        Endorsement e = buildEndorsement(EndorsementType.UPDATE, BigDecimal.ZERO);
        assertThat(EndorsementPriority.classify(e)).isEqualTo(EndorsementPriority.P1_COST_NEUTRAL);
    }

    @Test
    @DisplayName("ADD type classified as P2_ADDITION")
    void classify_AddType_P2() {
        Endorsement e = buildEndorsement(EndorsementType.ADD, new BigDecimal("500"));
        assertThat(EndorsementPriority.classify(e)).isEqualTo(EndorsementPriority.P2_ADDITION);
    }

    @Test
    @DisplayName("UPDATE type with positive premium classified as P3_PREMIUM_UPDATE")
    void classify_UpdatePositivePremium_P3() {
        Endorsement e = buildEndorsement(EndorsementType.UPDATE, new BigDecimal("200"));
        assertThat(EndorsementPriority.classify(e)).isEqualTo(EndorsementPriority.P3_PREMIUM_UPDATE);
    }

    @Test
    @DisplayName("priority ranks are ordered correctly")
    void ranks_AreOrdered() {
        assertThat(EndorsementPriority.P0_DELETION.getRank())
                .isLessThan(EndorsementPriority.P1_COST_NEUTRAL.getRank());
        assertThat(EndorsementPriority.P1_COST_NEUTRAL.getRank())
                .isLessThan(EndorsementPriority.P2_ADDITION.getRank());
        assertThat(EndorsementPriority.P2_ADDITION.getRank())
                .isLessThan(EndorsementPriority.P3_PREMIUM_UPDATE.getRank());
    }
}
