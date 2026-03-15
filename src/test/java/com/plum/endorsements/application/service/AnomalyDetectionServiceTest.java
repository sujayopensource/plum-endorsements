package com.plum.endorsements.application.service;

import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnomalyDetectionService")
class AnomalyDetectionServiceTest {

    @Mock private AnomalyDetectionPort anomalyDetector;
    @Mock private AnomalyDetectionRepository anomalyRepository;
    @Mock private EndorsementRepository endorsementRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private NotificationPort notificationPort;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    @InjectMocks
    private AnomalyDetectionService service;

    private UUID endorsementId;
    private UUID employerId;

    @BeforeEach
    void setUp() {
        endorsementId = UUID.randomUUID();
        employerId = UUID.randomUUID();
        ReflectionTestUtils.setField(service, "minAnomalyScore", 0.7);
    }

    private Endorsement buildEndorsement() {
        return Endorsement.builder()
                .id(endorsementId)
                .employerId(employerId)
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(5))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("analyzeEndorsement flags anomaly when score is above threshold")
    void analyzeEndorsement_ScoreAboveThreshold_FlagsAnomaly() {
        Endorsement endorsement = buildEndorsement();
        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.of(endorsement));
        when(endorsementRepository.findByStatus(any())).thenReturn(new ArrayList<>());

        when(anomalyDetector.analyzeEndorsement(eq(endorsement), anyList()))
                .thenReturn(new AnomalyDetectionPort.AnomalyResult("VOLUME_SPIKE", 0.85, "Spike detected"));

        when(anomalyRepository.save(any(AnomalyDetection.class))).thenAnswer(i -> {
            AnomalyDetection a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        service.analyzeEndorsement(endorsementId);

        verify(anomalyRepository).save(argThat(anomaly ->
                anomaly.getAnomalyType() == AnomalyType.VOLUME_SPIKE
                        && anomaly.getScore() == 0.85
                        && anomaly.getStatus() == AnomalyStatus.FLAGGED
                        && anomaly.getEmployerId().equals(employerId)
        ));
        verify(eventPublisher).publish(any(EndorsementEvent.AnomalyDetected.class));
        verify(notificationPort).notifyAnomalyDetected(
                eq(employerId), eq("VOLUME_SPIKE"), eq(0.85), eq("Spike detected"));
    }

    @Test
    @DisplayName("analyzeEndorsement does not flag when score is below threshold")
    void analyzeEndorsement_ScoreBelowThreshold_DoesNotFlag() {
        Endorsement endorsement = buildEndorsement();
        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.of(endorsement));
        when(endorsementRepository.findByStatus(any())).thenReturn(new ArrayList<>());

        when(anomalyDetector.analyzeEndorsement(eq(endorsement), anyList()))
                .thenReturn(new AnomalyDetectionPort.AnomalyResult("VOLUME_SPIKE", 0.3, "Normal volume"));

        service.analyzeEndorsement(endorsementId);

        verify(anomalyRepository, never()).save(any(AnomalyDetection.class));
        verify(eventPublisher, never()).publish(any(EndorsementEvent.AnomalyDetected.class));
        verify(notificationPort, never()).notifyAnomalyDetected(any(), any(), anyDouble(), any());
    }

    @Test
    @DisplayName("analyzeEndorsement does nothing when endorsement not found")
    void analyzeEndorsement_EndorsementNotFound_NoOp() {
        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.empty());

        service.analyzeEndorsement(endorsementId);

        verify(anomalyDetector, never()).analyzeEndorsement(any(), anyList());
        verify(anomalyRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("runBatchAnalysis processes recently created endorsements")
    void runBatchAnalysis_ProcessesRecentEndorsements() {
        Endorsement recent = buildEndorsement();
        recent.setCreatedAt(Instant.now()); // Just created

        // Return fresh mutable lists on each call to avoid ConcurrentModificationException
        when(endorsementRepository.findByStatus(EndorsementStatus.CREATED))
                .thenAnswer(inv -> new ArrayList<>(List.of(recent)));
        when(endorsementRepository.findByStatus(EndorsementStatus.VALIDATED))
                .thenAnswer(inv -> new ArrayList<>());
        when(endorsementRepository.findByStatus(EndorsementStatus.PROVISIONALLY_COVERED))
                .thenAnswer(inv -> new ArrayList<>());
        when(endorsementRepository.findByStatus(EndorsementStatus.CONFIRMED))
                .thenAnswer(inv -> new ArrayList<>());
        when(endorsementRepository.findById(endorsementId))
                .thenReturn(Optional.of(recent));
        when(anomalyDetector.analyzeEndorsement(any(), anyList()))
                .thenReturn(new AnomalyDetectionPort.AnomalyResult("NONE", 0.1, "Normal"));

        service.runBatchAnalysis();

        // Verify analyzeEndorsement was called (via findById)
        verify(endorsementRepository, atLeastOnce()).findById(endorsementId);
    }

    @Test
    @DisplayName("reviewAnomaly transitions status correctly")
    void reviewAnomaly_TransitionsStatus() {
        UUID anomalyId = UUID.randomUUID();
        AnomalyDetection anomaly = AnomalyDetection.builder()
                .id(anomalyId)
                .endorsementId(endorsementId)
                .employerId(employerId)
                .anomalyType(AnomalyType.SUSPICIOUS_TIMING)
                .score(0.75)
                .status(AnomalyStatus.FLAGGED)
                .flaggedAt(Instant.now())
                .build();

        when(anomalyRepository.findById(anomalyId)).thenReturn(Optional.of(anomaly));
        when(anomalyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AnomalyDetection result = service.reviewAnomaly(anomalyId, AnomalyStatus.UNDER_REVIEW, "Investigating");

        assertThat(result.getStatus()).isEqualTo(AnomalyStatus.UNDER_REVIEW);
        assertThat(result.getReviewerNotes()).isEqualTo("Investigating");
        verify(anomalyRepository).save(anomaly);
    }

    @Test
    @DisplayName("reviewAnomaly throws when anomaly not found")
    void reviewAnomaly_NotFound_Throws() {
        UUID anomalyId = UUID.randomUUID();
        when(anomalyRepository.findById(anomalyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reviewAnomaly(anomalyId, AnomalyStatus.DISMISSED, "Not found"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Anomaly not found");
    }

    // --- Phase 3 Edge Case Tests ---

    @Test
    @DisplayName("analyzeEndorsement handles multiple simultaneous anomalies for same employer")
    void shouldHandleMultipleSimultaneousAnomalies() {
        UUID endorsementId2 = UUID.randomUUID();
        Endorsement endorsement1 = buildEndorsement();
        Endorsement endorsement2 = buildEndorsement();
        endorsement2.setId(endorsementId2);

        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.of(endorsement1));
        when(endorsementRepository.findById(endorsementId2)).thenReturn(Optional.of(endorsement2));
        when(endorsementRepository.findByStatus(any())).thenReturn(new ArrayList<>());

        // Both endorsements flag anomalies above threshold
        when(anomalyDetector.analyzeEndorsement(eq(endorsement1), anyList()))
                .thenReturn(new AnomalyDetectionPort.AnomalyResult("VOLUME_SPIKE", 0.90, "Spike for endorsement 1"));
        when(anomalyDetector.analyzeEndorsement(eq(endorsement2), anyList()))
                .thenReturn(new AnomalyDetectionPort.AnomalyResult("SUSPICIOUS_TIMING", 0.80, "Timing for endorsement 2"));

        when(anomalyRepository.save(any(AnomalyDetection.class))).thenAnswer(i -> {
            AnomalyDetection a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        service.analyzeEndorsement(endorsementId);
        service.analyzeEndorsement(endorsementId2);

        // Both should be saved and events published
        verify(anomalyRepository, times(2)).save(any(AnomalyDetection.class));
        verify(eventPublisher, times(2)).publish(any(EndorsementEvent.AnomalyDetected.class));
    }

    @Test
    @DisplayName("analyzeEndorsement does not publish event when score is below threshold")
    void shouldNotPublishEventBelowThreshold() {
        Endorsement endorsement = buildEndorsement();
        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.of(endorsement));
        when(endorsementRepository.findByStatus(any())).thenReturn(new ArrayList<>());

        // Score at 0.69, just below the 0.7 threshold
        when(anomalyDetector.analyzeEndorsement(eq(endorsement), anyList()))
                .thenReturn(new AnomalyDetectionPort.AnomalyResult("VOLUME_SPIKE", 0.69, "Borderline normal"));

        service.analyzeEndorsement(endorsementId);

        verify(anomalyRepository, never()).save(any(AnomalyDetection.class));
        verify(eventPublisher, never()).publish(any(EndorsementEvent.AnomalyDetected.class));
        // Metrics should still be recorded even when no anomaly is flagged
        verify(meterRegistry, atLeastOnce()).summary(eq("endorsement.anomaly.score"),
                eq("anomalyType"), eq("VOLUME_SPIKE"));
    }

    @Test
    @DisplayName("reviewAnomaly records false positive rate when dismissed")
    void reviewAnomaly_Dismissed_RecordsFalsePositiveRate() {
        UUID anomalyId = UUID.randomUUID();
        AnomalyDetection anomaly = AnomalyDetection.builder()
                .id(anomalyId)
                .endorsementId(endorsementId)
                .employerId(employerId)
                .anomalyType(AnomalyType.VOLUME_SPIKE)
                .score(0.85)
                .status(AnomalyStatus.UNDER_REVIEW)
                .flaggedAt(Instant.now())
                .build();

        when(anomalyRepository.findById(anomalyId)).thenReturn(Optional.of(anomaly));
        when(anomalyRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(anomalyRepository.countByAnomalyTypeAndStatus(AnomalyType.VOLUME_SPIKE, AnomalyStatus.DISMISSED))
                .thenReturn(3L);
        when(anomalyRepository.countByAnomalyTypeAndStatus(AnomalyType.VOLUME_SPIKE, AnomalyStatus.CONFIRMED_FRAUD))
                .thenReturn(1L);

        AnomalyDetection result = service.reviewAnomaly(anomalyId, AnomalyStatus.DISMISSED, "False alarm");

        assertThat(result.getStatus()).isEqualTo(AnomalyStatus.DISMISSED);
        verify(meterRegistry).counter("endorsement.anomaly.review",
                "anomalyType", "VOLUME_SPIKE", "outcome", "false_positive");
        verify(meterRegistry).gauge(eq("endorsement.anomaly.false_positive_rate.volume_spike"), eq(0.75));
    }

    @Test
    @DisplayName("reviewAnomaly records true positive when confirmed fraud")
    void reviewAnomaly_ConfirmedFraud_RecordsTruePositive() {
        UUID anomalyId = UUID.randomUUID();
        AnomalyDetection anomaly = AnomalyDetection.builder()
                .id(anomalyId)
                .endorsementId(endorsementId)
                .employerId(employerId)
                .anomalyType(AnomalyType.SUSPICIOUS_TIMING)
                .score(0.90)
                .status(AnomalyStatus.UNDER_REVIEW)
                .flaggedAt(Instant.now())
                .build();

        when(anomalyRepository.findById(anomalyId)).thenReturn(Optional.of(anomaly));
        when(anomalyRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(anomalyRepository.countByAnomalyTypeAndStatus(AnomalyType.SUSPICIOUS_TIMING, AnomalyStatus.DISMISSED))
                .thenReturn(1L);
        when(anomalyRepository.countByAnomalyTypeAndStatus(AnomalyType.SUSPICIOUS_TIMING, AnomalyStatus.CONFIRMED_FRAUD))
                .thenReturn(4L);

        AnomalyDetection result = service.reviewAnomaly(anomalyId, AnomalyStatus.CONFIRMED_FRAUD, "Verified fraud");

        assertThat(result.getStatus()).isEqualTo(AnomalyStatus.CONFIRMED_FRAUD);
        verify(meterRegistry).counter("endorsement.anomaly.review",
                "anomalyType", "SUSPICIOUS_TIMING", "outcome", "true_positive");
        // 1 dismissed / 5 total = 0.2 false positive rate
        verify(meterRegistry).gauge(eq("endorsement.anomaly.false_positive_rate.suspicious_timing"), eq(0.2));
    }

    @Test
    @DisplayName("analyzeEndorsement records metrics via MeterRegistry on every analysis")
    void shouldRecordMetricsOnAnalysis() {
        Endorsement endorsement = buildEndorsement();
        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.of(endorsement));
        when(endorsementRepository.findByStatus(any())).thenReturn(new ArrayList<>());

        when(anomalyDetector.analyzeEndorsement(eq(endorsement), anyList()))
                .thenReturn(new AnomalyDetectionPort.AnomalyResult("SUSPICIOUS_TIMING", 0.45, "Normal timing"));

        service.analyzeEndorsement(endorsementId);

        // MeterRegistry.summary should be called to record the score
        verify(meterRegistry).summary("endorsement.anomaly.score",
                "anomalyType", "SUSPICIOUS_TIMING");
    }
}
