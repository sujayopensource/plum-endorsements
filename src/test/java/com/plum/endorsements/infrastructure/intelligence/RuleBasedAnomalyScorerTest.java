package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.Endorsement;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.EndorsementType;
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

@DisplayName("RuleBasedAnomalyScorer")
class RuleBasedAnomalyScorerTest {

    private RuleBasedAnomalyScorer scorer;
    private UUID employerId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        scorer = new RuleBasedAnomalyScorer();
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
    void score_volumeSpike_detectsHighVolume() {
        Endorsement target = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now());

        List<Endorsement> history = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            history.add(buildEndorsement(EndorsementType.ADD,
                    new BigDecimal("1000.00"), Instant.now().minus(i, ChronoUnit.HOURS)));
        }

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, history);

        assertThat(result.score()).isGreaterThan(0.5);
        assertThat(result.anomalyType()).isEqualTo("VOLUME_SPIKE");
    }

    @Test
    @DisplayName("detects add/delete cycling for same employee")
    void score_addDeleteCycling_detects() {
        Endorsement target = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now());

        List<Endorsement> history = new ArrayList<>();
        history.add(buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now().minus(10, ChronoUnit.DAYS)));
        history.add(buildEndorsement(EndorsementType.DELETE,
                new BigDecimal("1000.00"), Instant.now().minus(5, ChronoUnit.DAYS)));

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, history);

        assertThat(result.score()).isGreaterThanOrEqualTo(0.85);
        assertThat(result.anomalyType()).isEqualTo("ADD_DELETE_CYCLING");
    }

    @Test
    @DisplayName("detects suspicious timing for near-term coverage")
    void score_suspiciousTiming_detectsOutOfHours() {
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID())
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

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, new ArrayList<>());

        assertThat(result.score()).isGreaterThanOrEqualTo(0.7);
        assertThat(result.anomalyType()).isEqualTo("SUSPICIOUS_TIMING");
    }

    @Test
    @DisplayName("detects unusual premium using statistical outlier analysis")
    void score_unusualPremium_detectsOutlier() {
        Endorsement target = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("50000.00"), Instant.now());
        target.setEmployeeId(UUID.randomUUID());

        List<Endorsement> history = new ArrayList<>();
        BigDecimal[] premiums = {
                new BigDecimal("900.00"), new BigDecimal("1000.00"), new BigDecimal("1100.00"),
                new BigDecimal("950.00"), new BigDecimal("1050.00"), new BigDecimal("980.00"),
                new BigDecimal("1020.00"), new BigDecimal("970.00"), new BigDecimal("1030.00"),
                new BigDecimal("990.00")
        };
        for (int i = 0; i < 10; i++) {
            Endorsement h = buildEndorsement(EndorsementType.ADD, premiums[i],
                    Instant.now().minus(i + 10, ChronoUnit.DAYS));
            h.setEmployeeId(UUID.randomUUID());
            history.add(h);
        }

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, history);

        assertThat(result.score()).isGreaterThan(0.0);
        assertThat(result.anomalyType()).isEqualTo("UNUSUAL_PREMIUM");
    }

    @Test
    @DisplayName("returns low score for normal endorsement")
    void score_normalEndorsement_returnsLowScore() {
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.UPDATE)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(60))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, new ArrayList<>());

        assertThat(result.score()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("returns zero score with empty history")
    void score_emptyHistory_returnsZero() {
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.UPDATE)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(90))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, new ArrayList<>());

        assertThat(result).isNotNull();
        assertThat(result.score()).isEqualTo(0.0);
        assertThat(result.anomalyType()).isNotNull();
    }

    @Test
    @DisplayName("handles null premium gracefully")
    void score_nullEndorsement_handlesGracefully() {
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(UUID.randomUUID())
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.UPDATE)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(60))
                .premiumAmount(null)
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, new ArrayList<>());

        assertThat(result).isNotNull();
        assertThat(result.score()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("returns highest score when multiple rules trigger")
    void score_multipleRulesTriggered_returnsHighestScore() {
        // ADD type with near-term coverage (triggers SUSPICIOUS_TIMING at 0.75)
        // plus ADD/DELETE cycling (triggers at 0.85)
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(employeeId)
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
        history.add(buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now().minus(5, ChronoUnit.DAYS)));
        history.add(buildEndorsement(EndorsementType.DELETE,
                new BigDecimal("1000.00"), Instant.now().minus(3, ChronoUnit.DAYS)));

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, history);

        // ADD_DELETE_CYCLING (0.85) > SUSPICIOUS_TIMING (0.75)
        assertThat(result.score()).isGreaterThanOrEqualTo(0.85);
        assertThat(result.anomalyType()).isEqualTo("ADD_DELETE_CYCLING");
    }

    // ── Dormancy Break Rule Tests ──

    @Test
    @DisplayName("dormancy break returns zero when no history for employee")
    void checkDormancyBreak_noHistoryForEmployee_returnsZero() {
        Endorsement target = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now());
        // Different employee in history
        Endorsement historyEntry = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now().minus(10, ChronoUnit.DAYS));
        historyEntry.setEmployeeId(UUID.randomUUID());

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, List.of(historyEntry));

        // Dormancy break should not fire because no history for the target employee
        assertThat(result.anomalyType()).isNotEqualTo("DORMANCY_BREAK");
    }

    @Test
    @DisplayName("dormancy break returns zero when recent activity within 30 days")
    void checkDormancyBreak_recentActivity30Days_returnsZero() {
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(employeeId)
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.UPDATE)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(60))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Endorsement historyEntry = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now().minus(30, ChronoUnit.DAYS));

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, List.of(historyEntry));

        // Recent activity means no dormancy break, and no other rules trigger
        assertThat(result.score()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("dormancy break detected when 91 days inactive")
    void checkDormancyBreak_91DaysInactive_returnsDormancyBreak() {
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(employeeId)
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.UPDATE)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(60))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Endorsement historyEntry = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now().minus(91, ChronoUnit.DAYS));

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, List.of(historyEntry));

        assertThat(result.anomalyType()).isEqualTo("DORMANCY_BREAK");
        assertThat(result.score()).isGreaterThan(0.6);
    }

    @Test
    @DisplayName("dormancy break score capped at 0.85 for 365 days inactive")
    void checkDormancyBreak_365DaysInactive_returnsCappedAt085() {
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(employeeId)
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.UPDATE)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(60))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Endorsement historyEntry = buildEndorsement(EndorsementType.ADD,
                new BigDecimal("1000.00"), Instant.now().minus(365, ChronoUnit.DAYS));

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, List.of(historyEntry));

        assertThat(result.anomalyType()).isEqualTo("DORMANCY_BREAK");
        assertThat(result.score()).isLessThanOrEqualTo(0.85);
    }

    @Test
    @DisplayName("dormancy break is highest score when other rules have lower scores")
    void score_dormancyBreakIsHighest_returnsDormancyBreak() {
        // UPDATE type (no suspicious timing), no volume spike, no cycling
        // Only dormancy break triggers
        Endorsement target = Endorsement.builder()
                .id(UUID.randomUUID())
                .employerId(employerId)
                .employeeId(employeeId)
                .insurerId(UUID.randomUUID())
                .policyId(UUID.randomUUID())
                .type(EndorsementType.UPDATE)
                .status(EndorsementStatus.CREATED)
                .coverageStartDate(LocalDate.now().plusDays(60))
                .premiumAmount(new BigDecimal("1000.00"))
                .retryCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Same employee, 200 days ago - dormancy break
        Endorsement historyEntry = buildEndorsement(EndorsementType.UPDATE,
                new BigDecimal("1000.00"), Instant.now().minus(200, ChronoUnit.DAYS));

        RuleBasedAnomalyScorer.ScoringResult result = scorer.score(target, List.of(historyEntry));

        assertThat(result.anomalyType()).isEqualTo("DORMANCY_BREAK");
        assertThat(result.score()).isGreaterThan(0.0);
    }
}
