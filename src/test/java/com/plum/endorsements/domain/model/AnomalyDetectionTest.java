package com.plum.endorsements.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AnomalyDetection domain model")
class AnomalyDetectionTest {

    private AnomalyDetection buildAnomaly(AnomalyStatus status) {
        return AnomalyDetection.builder()
                .id(UUID.randomUUID())
                .endorsementId(UUID.randomUUID())
                .employerId(UUID.randomUUID())
                .anomalyType(AnomalyType.VOLUME_SPIKE)
                .score(0.85)
                .explanation("Volume spike detected")
                .flaggedAt(Instant.now())
                .status(status)
                .build();
    }

    @Test
    @DisplayName("builder sets all fields correctly")
    void builder_SetsAllFields() {
        UUID id = UUID.randomUUID();
        UUID endorsementId = UUID.randomUUID();
        UUID employerId = UUID.randomUUID();
        Instant flaggedAt = Instant.now();

        AnomalyDetection anomaly = AnomalyDetection.builder()
                .id(id)
                .endorsementId(endorsementId)
                .employerId(employerId)
                .anomalyType(AnomalyType.ADD_DELETE_CYCLING)
                .score(0.92)
                .explanation("Cycling detected")
                .flaggedAt(flaggedAt)
                .status(AnomalyStatus.FLAGGED)
                .build();

        assertThat(anomaly.getId()).isEqualTo(id);
        assertThat(anomaly.getEndorsementId()).isEqualTo(endorsementId);
        assertThat(anomaly.getEmployerId()).isEqualTo(employerId);
        assertThat(anomaly.getAnomalyType()).isEqualTo(AnomalyType.ADD_DELETE_CYCLING);
        assertThat(anomaly.getScore()).isEqualTo(0.92);
        assertThat(anomaly.getExplanation()).isEqualTo("Cycling detected");
        assertThat(anomaly.getFlaggedAt()).isEqualTo(flaggedAt);
        assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.FLAGGED);
        assertThat(anomaly.getReviewedAt()).isNull();
        assertThat(anomaly.getReviewerNotes()).isNull();
    }

    @Test
    @DisplayName("FLAGGED -> UNDER_REVIEW transition succeeds")
    void review_FlaggedToUnderReview_Succeeds() {
        AnomalyDetection anomaly = buildAnomaly(AnomalyStatus.FLAGGED);

        anomaly.review(AnomalyStatus.UNDER_REVIEW, "Starting investigation");

        assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.UNDER_REVIEW);
        assertThat(anomaly.getReviewedAt()).isNotNull();
        assertThat(anomaly.getReviewerNotes()).isEqualTo("Starting investigation");
    }

    @Test
    @DisplayName("UNDER_REVIEW -> DISMISSED transition succeeds")
    void review_UnderReviewToDismissed_Succeeds() {
        AnomalyDetection anomaly = buildAnomaly(AnomalyStatus.UNDER_REVIEW);

        anomaly.review(AnomalyStatus.DISMISSED, "False positive");

        assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.DISMISSED);
        assertThat(anomaly.getReviewerNotes()).isEqualTo("False positive");
    }

    @Test
    @DisplayName("UNDER_REVIEW -> CONFIRMED_FRAUD transition succeeds")
    void review_UnderReviewToConfirmedFraud_Succeeds() {
        AnomalyDetection anomaly = buildAnomaly(AnomalyStatus.UNDER_REVIEW);

        anomaly.review(AnomalyStatus.CONFIRMED_FRAUD, "Confirmed fraudulent activity");

        assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.CONFIRMED_FRAUD);
        assertThat(anomaly.getReviewedAt()).isNotNull();
        assertThat(anomaly.getReviewerNotes()).isEqualTo("Confirmed fraudulent activity");
    }

    @Test
    @DisplayName("invalid transition from FLAGGED to CONFIRMED_FRAUD throws")
    void review_FlaggedToConfirmedFraud_Throws() {
        AnomalyDetection anomaly = buildAnomaly(AnomalyStatus.FLAGGED);

        assertThatThrownBy(() -> anomaly.review(AnomalyStatus.CONFIRMED_FRAUD, "Skip review"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition anomaly from FLAGGED to CONFIRMED_FRAUD");
    }

    @Test
    @DisplayName("invalid transition from DISMISSED throws (terminal state)")
    void review_DismissedToAny_Throws() {
        AnomalyDetection anomaly = buildAnomaly(AnomalyStatus.DISMISSED);

        assertThatThrownBy(() -> anomaly.review(AnomalyStatus.UNDER_REVIEW, "Reopen"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition anomaly from DISMISSED to UNDER_REVIEW");
    }
}
