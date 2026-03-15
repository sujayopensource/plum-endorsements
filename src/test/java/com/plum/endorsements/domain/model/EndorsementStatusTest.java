package com.plum.endorsements.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

class EndorsementStatusTest {

    // --- Valid transition tests ---

    @Test
    void created_canTransitionTo_validated() {
        assertThat(EndorsementStatus.CREATED.canTransitionTo(EndorsementStatus.VALIDATED)).isTrue();
    }

    @Test
    void validated_canTransitionTo_provisionallyCovered() {
        assertThat(EndorsementStatus.VALIDATED.canTransitionTo(EndorsementStatus.PROVISIONALLY_COVERED)).isTrue();
    }

    @Test
    void provisionallyCovered_canTransitionTo_submittedRealtime() {
        assertThat(EndorsementStatus.PROVISIONALLY_COVERED.canTransitionTo(EndorsementStatus.SUBMITTED_REALTIME)).isTrue();
    }

    @Test
    void provisionallyCovered_canTransitionTo_queuedForBatch() {
        assertThat(EndorsementStatus.PROVISIONALLY_COVERED.canTransitionTo(EndorsementStatus.QUEUED_FOR_BATCH)).isTrue();
    }

    @Test
    void submittedRealtime_canTransitionTo_insurerProcessing() {
        assertThat(EndorsementStatus.SUBMITTED_REALTIME.canTransitionTo(EndorsementStatus.INSURER_PROCESSING)).isTrue();
    }

    @Test
    void submittedRealtime_canTransitionTo_rejected() {
        assertThat(EndorsementStatus.SUBMITTED_REALTIME.canTransitionTo(EndorsementStatus.REJECTED)).isTrue();
    }

    @Test
    void queuedForBatch_canTransitionTo_batchSubmitted() {
        assertThat(EndorsementStatus.QUEUED_FOR_BATCH.canTransitionTo(EndorsementStatus.BATCH_SUBMITTED)).isTrue();
    }

    @Test
    void batchSubmitted_canTransitionTo_insurerProcessing() {
        assertThat(EndorsementStatus.BATCH_SUBMITTED.canTransitionTo(EndorsementStatus.INSURER_PROCESSING)).isTrue();
    }

    @Test
    void batchSubmitted_canTransitionTo_rejected() {
        assertThat(EndorsementStatus.BATCH_SUBMITTED.canTransitionTo(EndorsementStatus.REJECTED)).isTrue();
    }

    @Test
    void insurerProcessing_canTransitionTo_confirmed() {
        assertThat(EndorsementStatus.INSURER_PROCESSING.canTransitionTo(EndorsementStatus.CONFIRMED)).isTrue();
    }

    @Test
    void insurerProcessing_canTransitionTo_rejected() {
        assertThat(EndorsementStatus.INSURER_PROCESSING.canTransitionTo(EndorsementStatus.REJECTED)).isTrue();
    }

    @Test
    void rejected_canTransitionTo_retryPending() {
        assertThat(EndorsementStatus.REJECTED.canTransitionTo(EndorsementStatus.RETRY_PENDING)).isTrue();
    }

    @Test
    void rejected_canTransitionTo_failedPermanent() {
        assertThat(EndorsementStatus.REJECTED.canTransitionTo(EndorsementStatus.FAILED_PERMANENT)).isTrue();
    }

    @Test
    void retryPending_canTransitionTo_submittedRealtime() {
        assertThat(EndorsementStatus.RETRY_PENDING.canTransitionTo(EndorsementStatus.SUBMITTED_REALTIME)).isTrue();
    }

    @Test
    void retryPending_canTransitionTo_queuedForBatch() {
        assertThat(EndorsementStatus.RETRY_PENDING.canTransitionTo(EndorsementStatus.QUEUED_FOR_BATCH)).isTrue();
    }

    @Test
    void retryPending_canTransitionTo_failedPermanent() {
        assertThat(EndorsementStatus.RETRY_PENDING.canTransitionTo(EndorsementStatus.FAILED_PERMANENT)).isTrue();
    }

    // --- Terminal state tests ---

    @Test
    void confirmed_isTerminal() {
        assertThat(EndorsementStatus.CONFIRMED.isTerminal()).isTrue();
    }

    @Test
    void failedPermanent_isTerminal() {
        assertThat(EndorsementStatus.FAILED_PERMANENT.isTerminal()).isTrue();
    }

    @Test
    void created_isNotTerminal() {
        assertThat(EndorsementStatus.CREATED.isTerminal()).isFalse();
    }

    @Test
    void validated_isNotTerminal() {
        assertThat(EndorsementStatus.VALIDATED.isTerminal()).isFalse();
    }

    @Test
    void rejected_isNotTerminal() {
        assertThat(EndorsementStatus.REJECTED.isTerminal()).isFalse();
    }

    // --- isActive is inverse of isTerminal ---

    @Test
    void confirmed_isNotActive() {
        assertThat(EndorsementStatus.CONFIRMED.isActive()).isFalse();
    }

    @Test
    void failedPermanent_isNotActive() {
        assertThat(EndorsementStatus.FAILED_PERMANENT.isActive()).isFalse();
    }

    @Test
    void created_isActive() {
        assertThat(EndorsementStatus.CREATED.isActive()).isTrue();
    }

    // --- Invalid transition tests ---

    @Test
    void created_cannotTransitionTo_confirmed() {
        assertThat(EndorsementStatus.CREATED.canTransitionTo(EndorsementStatus.CONFIRMED)).isFalse();
    }

    @Test
    void created_cannotTransitionTo_rejected() {
        assertThat(EndorsementStatus.CREATED.canTransitionTo(EndorsementStatus.REJECTED)).isFalse();
    }

    @Test
    void created_cannotTransitionTo_provisionallyCovered() {
        assertThat(EndorsementStatus.CREATED.canTransitionTo(EndorsementStatus.PROVISIONALLY_COVERED)).isFalse();
    }

    @Test
    void validated_cannotTransitionTo_confirmed() {
        assertThat(EndorsementStatus.VALIDATED.canTransitionTo(EndorsementStatus.CONFIRMED)).isFalse();
    }

    @Test
    void confirmed_cannotTransitionToAnything() {
        for (EndorsementStatus status : EndorsementStatus.values()) {
            assertThat(EndorsementStatus.CONFIRMED.canTransitionTo(status))
                    .as("CONFIRMED should not transition to %s", status)
                    .isFalse();
        }
    }

    @Test
    void failedPermanent_cannotTransitionToAnything() {
        for (EndorsementStatus status : EndorsementStatus.values()) {
            assertThat(EndorsementStatus.FAILED_PERMANENT.canTransitionTo(status))
                    .as("FAILED_PERMANENT should not transition to %s", status)
                    .isFalse();
        }
    }

    @Test
    void rejected_cannotTransitionTo_confirmed() {
        assertThat(EndorsementStatus.REJECTED.canTransitionTo(EndorsementStatus.CONFIRMED)).isFalse();
    }

    @Test
    void rejected_cannotTransitionTo_created() {
        assertThat(EndorsementStatus.REJECTED.canTransitionTo(EndorsementStatus.CREATED)).isFalse();
    }

    // --- requiresInsurerAction tests ---

    @Test
    void submittedRealtime_requiresInsurerAction() {
        assertThat(EndorsementStatus.SUBMITTED_REALTIME.requiresInsurerAction()).isTrue();
    }

    @Test
    void batchSubmitted_requiresInsurerAction() {
        assertThat(EndorsementStatus.BATCH_SUBMITTED.requiresInsurerAction()).isTrue();
    }

    @Test
    void insurerProcessing_requiresInsurerAction() {
        assertThat(EndorsementStatus.INSURER_PROCESSING.requiresInsurerAction()).isTrue();
    }

    @Test
    void created_doesNotRequireInsurerAction() {
        assertThat(EndorsementStatus.CREATED.requiresInsurerAction()).isFalse();
    }

    @Test
    void validated_doesNotRequireInsurerAction() {
        assertThat(EndorsementStatus.VALIDATED.requiresInsurerAction()).isFalse();
    }

    @Test
    void provisionallyCovered_doesNotRequireInsurerAction() {
        assertThat(EndorsementStatus.PROVISIONALLY_COVERED.requiresInsurerAction()).isFalse();
    }

    @Test
    void confirmed_doesNotRequireInsurerAction() {
        assertThat(EndorsementStatus.CONFIRMED.requiresInsurerAction()).isFalse();
    }

    @Test
    void rejected_doesNotRequireInsurerAction() {
        assertThat(EndorsementStatus.REJECTED.requiresInsurerAction()).isFalse();
    }

    @Test
    void failedPermanent_doesNotRequireInsurerAction() {
        assertThat(EndorsementStatus.FAILED_PERMANENT.requiresInsurerAction()).isFalse();
    }

    @Test
    void retryPending_doesNotRequireInsurerAction() {
        assertThat(EndorsementStatus.RETRY_PENDING.requiresInsurerAction()).isFalse();
    }

    @Test
    void queuedForBatch_doesNotRequireInsurerAction() {
        assertThat(EndorsementStatus.QUEUED_FOR_BATCH.requiresInsurerAction()).isFalse();
    }

    // --- Parameterized test: non-terminal statuses are active ---

    @ParameterizedTest
    @EnumSource(value = EndorsementStatus.class, names = {"CONFIRMED", "FAILED_PERMANENT"}, mode = EnumSource.Mode.EXCLUDE)
    void nonTerminalStatuses_areActive(EndorsementStatus status) {
        assertThat(status.isActive()).isTrue();
        assertThat(status.isTerminal()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EndorsementStatus.class, names = {"CONFIRMED", "FAILED_PERMANENT"})
    void terminalStatuses_areNotActive(EndorsementStatus status) {
        assertThat(status.isActive()).isFalse();
        assertThat(status.isTerminal()).isTrue();
    }
}
