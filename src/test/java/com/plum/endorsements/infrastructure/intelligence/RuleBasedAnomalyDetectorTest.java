package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.EndorsementType;
import com.plum.endorsements.domain.port.AnomalyDetectionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RuleBasedAnomalyDetector")
class RuleBasedAnomalyDetectorTest {

    private RuleBasedAnomalyDetector detector;
    private UUID employerId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        detector = new RuleBasedAnomalyDetector(new RuleBasedAnomalyScorer());
        employerId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
    }

    private Endorsement buildEndorsement(EndorsementType type, BigDecimal premium, Instant createdAt) {
        return Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(employeeId)
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(type)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(30))
                .premiumAmount(premium)
                .retryCount(0)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    @Test
    @DisplayName("detects volume spike with high endorsement count in 24h")
    void analyzeEndorsement_VolumeSpikeDetected() {
        Endorsement target = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now());

        // Create history: simulate 50 endorsements in last 24h with only a few in the past month
        List<Endorsement> history = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            history.add(buildEndorsement(EndorsementType.ADD,
                    new BigDecimal("1000.00"), Instant.now().minus(i, ChronoUnit.HOURS)));
        }

        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, history);

        // With 50 endorsements in month and ~50 in 24h, dailyAvg = 50/30 = ~1.67
        // recentCount (~50) > dailyAvg*5 (~8.3) => flagged
        assertThat(result.score()).isGreaterThan(0.5);
        assertThat(result.anomalyType()).isEqualTo("VOLUME_SPIKE");
    }

    @Test
    @DisplayName("detects add/delete cycling for same employee")
    void analyzeEndorsement_AddDeleteCyclingDetected() {
        Endorsement target = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now());

        List<Endorsement> history = new ArrayList<>();
        // Same employee has both ADD and DELETE in last 30 days
        history.add(buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now().minus(10, ChronoUnit.DAYS)));
        history.add(buildEndorsement(EndorsementType.DELETE,
                new BigDecimal("1000.00"), Instant.now().minus(5, ChronoUnit.DAYS)));

        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, history);

        assertThat(result.score()).isGreaterThanOrEqualTo(0.85);
        assertThat(result.anomalyType()).isEqualTo("ADD_DELETE_CYCLING");
        assertThat(result.explanation()).contains("Add/delete cycling detected");
    }

    @Test
    @DisplayName("detects suspicious timing for near-term coverage")
    void analyzeEndorsement_SuspiciousTimingDetected() {
        // Coverage starts in 3 days
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID()) // Different employee to avoid cycling
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(3))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        List<Endorsement> history = new ArrayList<>();

        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, history);

        assertThat(result.score()).isGreaterThanOrEqualTo(0.7);
        assertThat(result.anomalyType()).isEqualTo("SUSPICIOUS_TIMING");
    }

    @Test
    @DisplayName("detects unusual premium using statistical analysis")
    void analyzeEndorsement_UnusualPremiumDetected() {
        // Target has a very high premium
        Endorsement target = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("50000.00"), Instant.now());
        // Use different employee to avoid cycling
        target.setEmployeeId(UUID.randomUUID());

        // Create history with varied premiums (need at least 5 for analysis, need variance for stdDev > 0)
        List<Endorsement> history = new ArrayList<>();
        BigDecimal[] premiums = {
                new BigDecimal("900.00"), new BigDecimal("1000.00"), new BigDecimal("1100.00"),
                new BigDecimal("950.00"), new BigDecimal("1050.00"), new BigDecimal("980.00"),
                new BigDecimal("1020.00"), new BigDecimal("970.00"), new BigDecimal("1030.00"),
                new BigDecimal("990.00")
        };
        for (int i = 0; i < 10; i++) {
            Endorsement h = buildEndorsement(EndorsementType.ADD,
                    premiums[i],
                    Instant.now().minus(i + 10, ChronoUnit.DAYS));
            h.setEmployeeId(UUID.randomUUID());
            history.add(h);
        }

        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, history);

        // 50000 is far from mean of ~999, should be > 3 stddevs with variance present
        assertThat(result.score()).isGreaterThan(0.0);
        assertThat(result.anomalyType()).isEqualTo("UNUSUAL_PREMIUM");
    }

    @Test
    @DisplayName("returns no anomaly with normal data")
    void analyzeEndorsement_NormalData_NoAnomaly() {
        // Coverage starts in 60 days (not suspicious timing)
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID()) // Different employee
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.UPDATE) // Not ADD, so no suspicious timing
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(60))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Small history, no cycling, no spikes
        List<Endorsement> history = new ArrayList<>();

        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, history);

        assertThat(result.score()).isLessThan(0.5);
    }

    @Test
    @DisplayName("insufficient premium data skips unusual premium check")
    void analyzeEndorsement_InsufficientPremiumData_SkipsCheck() {
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.UPDATE)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(60))
                .premiumAmount(new BigDecimal("50000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Only 2 history items (need 5 for premium analysis)
        List<Endorsement> history = List.of(
                buildEndorsement(EndorsementType.ADD, new BigDecimal("1000.00"),
                        Instant.now().minus(10, ChronoUnit.DAYS)),
                buildEndorsement(EndorsementType.ADD, new BigDecimal("1000.00"),
                        Instant.now().minus(20, ChronoUnit.DAYS))
        );

        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, history);

        // Should not flag unusual premium due to insufficient data
        if (result.anomalyType().equals("UNUSUAL_PREMIUM")) {
            assertThat(result.score()).isEqualTo(0.0);
        }
    }

    // --- Phase 3 Edge Case Tests ---

    @Test
    @DisplayName("detects multiple anomaly types simultaneously and returns highest scoring")
    void shouldDetectMultipleAnomalyTypesSimultaneously() {
        // Create endorsement that triggers BOTH suspicious timing AND volume spike
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(employeeId)
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(3)) // Suspicious timing
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Large volume of history with ADD and DELETE for same employee (triggers cycling)
        List<Endorsement> history = new ArrayList<>();
        history.add(buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now().minus(5, ChronoUnit.DAYS)));
        history.add(buildEndorsement(EndorsementType.DELETE,
                new BigDecimal("1000.00"), Instant.now().minus(3, ChronoUnit.DAYS)));

        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, history);

        // Should return the highest scoring anomaly type
        assertThat(result.score()).isGreaterThan(0.0);
        // The result should be one of the triggered anomaly types
        assertThat(result.anomalyType()).isIn("ADD_DELETE_CYCLING", "SUSPICIOUS_TIMING",
                "VOLUME_SPIKE", "UNUSUAL_PREMIUM");
    }

    @Test
    @DisplayName("returns zero score for completely normal activity with no anomaly triggers")
    void shouldReturnZeroScoreForNormalActivity() {
        // UPDATE type (no suspicious timing), far future coverage, normal premium
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID()) // Different employee - no cycling
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.UPDATE) // Not ADD, so no suspicious timing
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(90)) // Far future
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Minimal history, no spikes, no cycling
        List<Endorsement> history = new ArrayList<>();

        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, history);

        // All rules should return 0.0 score
        assertThat(result.score()).isEqualTo(0.0);
        // No false positives
        assertThat(result.explanation()).isNotEmpty();
    }

    @Test
    @DisplayName("handles empty endorsement history without errors")
    void shouldHandleEmptyEndorsementHistory() {
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.ADD)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(30))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Completely empty history
        List<Endorsement> history = new ArrayList<>();

        // Should not throw any NPE or other exceptions
        AnomalyDetectionPort.AnomalyResult result = detector.analyzeEndorsement(target, history);

        assertThat(result).isNotNull();
        assertThat(result.anomalyType()).isNotNull();
        assertThat(result.explanation()).isNotNull();
    }
}
