package com.plum.endorsements.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class EndorsementTest {

    private Endorsement buildEndorsement(EndorsementStatus status) {
        return Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(status)
                .coverageStartDate(LocalDate.now())
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .idempotencyKey("idem-" + UUID.randomUUID())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // --- transitionTo tests ---

    @Test
    void transitionTo_validTransition_shouldSucceed() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.CREATED);
        Instant beforeTransition = endorsement.getUpdatedAt();

        endorsement.transitionTo(EndorsementStatus.VALIDATED);

        assertThat(endorsement.getStatus()).isEqualTo(EndorsementStatus.VALIDATED);
        assertThat(endorsement.getUpdatedAt()).isAfterOrEqualTo(beforeTransition);
    }

    @Test
    void transitionTo_validChain_shouldTraverseMultipleStates() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.CREATED);

        endorsement.transitionTo(EndorsementStatus.VALIDATED);
        endorsement.transitionTo(EndorsementStatus.PROVISIONALLY_COVERED);
        endorsement.transitionTo(EndorsementStatus.SUBMITTED_REALTIME);

        assertThat(endorsement.getStatus()).isEqualTo(EndorsementStatus.SUBMITTED_REALTIME);
    }

    @Test
    void transitionTo_invalidTransition_shouldThrowIllegalStateException() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.CREATED);

        assertThatThrownBy(() -> endorsement.transitionTo(EndorsementStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition from CREATED to CONFIRMED");
    }

    @Test
    void transitionTo_fromTerminalState_shouldThrowIllegalStateException() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.CONFIRMED);

        assertThatThrownBy(() -> endorsement.transitionTo(EndorsementStatus.REJECTED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition from CONFIRMED to REJECTED");
    }

    @Test
    void transitionTo_updatesTimestamp() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.CREATED);
        Instant originalUpdatedAt = Instant.parse("2024-01-01T00:00:00Z");
        endorsement.setUpdatedAt(originalUpdatedAt);

        endorsement.transitionTo(EndorsementStatus.VALIDATED);

        assertThat(endorsement.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    // --- canRetry tests ---

    @Test
    void canRetry_returnsTrueWhenRetryCountLessThan3AndStatusIsRejected() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.REJECTED);
        endorsement.setRetryCount(0);

        assertThat(endorsement.canRetry()).isTrue();
    }

    @Test
    void canRetry_returnsTrueWhenRetryCountIs2AndStatusIsRejected() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.REJECTED);
        endorsement.setRetryCount(2);

        assertThat(endorsement.canRetry()).isTrue();
    }

    @Test
    void canRetry_returnsFalseWhenRetryCountEquals3() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.REJECTED);
        endorsement.setRetryCount(3);

        assertThat(endorsement.canRetry()).isFalse();
    }

    @Test
    void canRetry_returnsFalseWhenRetryCountExceeds3() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.REJECTED);
        endorsement.setRetryCount(5);

        assertThat(endorsement.canRetry()).isFalse();
    }

    @Test
    void canRetry_returnsFalseWhenStatusIsNotRejected() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.CREATED);
        endorsement.setRetryCount(0);

        assertThat(endorsement.canRetry()).isFalse();
    }

    @Test
    void canRetry_returnsFalseWhenStatusIsConfirmed() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.CONFIRMED);
        endorsement.setRetryCount(0);

        assertThat(endorsement.canRetry()).isFalse();
    }

    @Test
    void canRetry_returnsFalseWhenStatusIsFailedPermanent() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.FAILED_PERMANENT);
        endorsement.setRetryCount(0);

        assertThat(endorsement.canRetry()).isFalse();
    }

    @Test
    void canRetry_returnsFalseWhenStatusIsRetryPending() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.RETRY_PENDING);
        endorsement.setRetryCount(1);

        assertThat(endorsement.canRetry()).isFalse();
    }

    // --- incrementRetry tests ---

    @Test
    void incrementRetry_incrementsCountAndSetsRetryPending() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.REJECTED);
        endorsement.setRetryCount(0);
        Instant beforeIncrement = Instant.now();

        endorsement.incrementRetry();

        assertThat(endorsement.getRetryCount()).isEqualTo(1);
        assertThat(endorsement.getStatus()).isEqualTo(EndorsementStatus.RETRY_PENDING);
        assertThat(endorsement.getUpdatedAt()).isAfterOrEqualTo(beforeIncrement);
    }

    @Test
    void incrementRetry_multipleIncrements() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.REJECTED);
        endorsement.setRetryCount(0);

        endorsement.incrementRetry();
        endorsement.incrementRetry();
        endorsement.incrementRetry();

        assertThat(endorsement.getRetryCount()).isEqualTo(3);
        assertThat(endorsement.getStatus()).isEqualTo(EndorsementStatus.RETRY_PENDING);
    }

    // --- isTerminal tests ---

    @Test
    void isTerminal_delegatesToStatus_confirmed() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.CONFIRMED);

        assertThat(endorsement.isTerminal()).isTrue();
    }

    @Test
    void isTerminal_delegatesToStatus_failedPermanent() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.FAILED_PERMANENT);

        assertThat(endorsement.isTerminal()).isTrue();
    }

    @Test
    void isTerminal_delegatesToStatus_created() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.CREATED);

        assertThat(endorsement.isTerminal()).isFalse();
    }

    @Test
    void isTerminal_delegatesToStatus_rejected() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.REJECTED);

        assertThat(endorsement.isTerminal()).isFalse();
    }

    @Test
    void isTerminal_delegatesToStatus_provisionallyCovered() {
        Endorsement endorsement = buildEndorsement(EndorsementStatus.PROVISIONALLY_COVERED);

        assertThat(endorsement.isTerminal()).isFalse();
    }
}
