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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployerHealthScoreService")
class EmployerHealthScoreServiceTest {

    @Mock private EndorsementRepository endorsementRepository;
    @Mock private EAAccountRepository eaAccountRepository;
    @Mock private AnomalyDetectionRepository anomalyDetectionRepository;
    @Mock private ReconciliationRepository reconciliationRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    @InjectMocks
    private EmployerHealthScoreService service;

    private UUID employerId;

    @BeforeEach
    void setUp() {
        employerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("calculateHealthScore returns perfect score when all indicators are healthy")
    void calculateHealthScore_AllHealthy_ReturnsPerfect() {
        // All confirmed, no rejected/failed
        when(endorsementRepository.countByEmployerIdAndStatus(eq(employerId), eq(EndorsementStatus.CONFIRMED)))
                .thenReturn(100L);
        when(endorsementRepository.countByEmployerIdAndStatus(eq(employerId), eq(EndorsementStatus.REJECTED)))
                .thenReturn(0L);
        when(endorsementRepository.countByEmployerIdAndStatus(eq(employerId), eq(EndorsementStatus.FAILED_PERMANENT)))
                .thenReturn(0L);

        // No anomalies
        when(anomalyDetectionRepository.countByEmployerIdAndFlaggedAtAfter(eq(employerId), any()))
                .thenReturn(0L);

        // Healthy EA balance
        when(eaAccountRepository.findByEmployerId(employerId))
                .thenReturn(List.of(EAAccount.builder().balance(new BigDecimal("100000")).build()));

        var score = service.calculateHealthScore(employerId);

        assertThat(score.overallScore()).isEqualByComparingTo(new BigDecimal("100.0"));
        assertThat(score.riskLevel()).isEqualTo("LOW");
        assertThat(score.endorsementSuccessRate()).isEqualByComparingTo(new BigDecimal("100.0"));
        assertThat(score.anomalyScore()).isEqualByComparingTo(new BigDecimal("100.0"));
    }

    @Test
    @DisplayName("calculateHealthScore returns HIGH risk when success rate is low and anomalies exist")
    void calculateHealthScore_LowSuccessHighAnomalies_ReturnsHighRisk() {
        // 50% confirmed, 50% rejected
        when(endorsementRepository.countByEmployerIdAndStatus(eq(employerId), eq(EndorsementStatus.CONFIRMED)))
                .thenReturn(50L);
        when(endorsementRepository.countByEmployerIdAndStatus(eq(employerId), eq(EndorsementStatus.REJECTED)))
                .thenReturn(50L);
        when(endorsementRepository.countByEmployerIdAndStatus(eq(employerId), eq(EndorsementStatus.FAILED_PERMANENT)))
                .thenReturn(0L);

        // Many anomalies
        when(anomalyDetectionRepository.countByEmployerIdAndFlaggedAtAfter(eq(employerId), any()))
                .thenReturn(10L);

        // Zero balance
        when(eaAccountRepository.findByEmployerId(employerId))
                .thenReturn(List.of(EAAccount.builder().balance(BigDecimal.ZERO).build()));

        var score = service.calculateHealthScore(employerId);

        assertThat(score.riskLevel()).isEqualTo("HIGH");
        assertThat(score.endorsementSuccessRate()).isEqualByComparingTo(new BigDecimal("50.0"));
        assertThat(score.anomalyScore()).isEqualByComparingTo(new BigDecimal("30.0"));
        assertThat(score.balanceHealthScore()).isEqualByComparingTo(new BigDecimal("0.0"));
    }

    @Test
    @DisplayName("calculateHealthScore returns 100% success rate when no endorsements exist")
    void calculateHealthScore_NoEndorsements_Returns100() {
        when(endorsementRepository.countByEmployerIdAndStatus(eq(employerId), any()))
                .thenReturn(0L);
        when(anomalyDetectionRepository.countByEmployerIdAndFlaggedAtAfter(eq(employerId), any()))
                .thenReturn(0L);
        when(eaAccountRepository.findByEmployerId(employerId))
                .thenReturn(List.of());

        var score = service.calculateHealthScore(employerId);

        assertThat(score.endorsementSuccessRate()).isEqualByComparingTo(new BigDecimal("100.0"));
    }

    @Test
    @DisplayName("calculateHealthScore returns MEDIUM risk for moderate issues")
    void calculateHealthScore_ModerateIssues_ReturnsMediumRisk() {
        // 80% success rate
        when(endorsementRepository.countByEmployerIdAndStatus(eq(employerId), eq(EndorsementStatus.CONFIRMED)))
                .thenReturn(80L);
        when(endorsementRepository.countByEmployerIdAndStatus(eq(employerId), eq(EndorsementStatus.REJECTED)))
                .thenReturn(20L);
        when(endorsementRepository.countByEmployerIdAndStatus(eq(employerId), eq(EndorsementStatus.FAILED_PERMANENT)))
                .thenReturn(0L);

        // 3 anomalies → 60 anomaly score
        when(anomalyDetectionRepository.countByEmployerIdAndFlaggedAtAfter(eq(employerId), any()))
                .thenReturn(3L);

        // 50% healthy balance
        when(eaAccountRepository.findByEmployerId(employerId))
                .thenReturn(List.of(
                        EAAccount.builder().balance(new BigDecimal("50000")).build(),
                        EAAccount.builder().balance(BigDecimal.ZERO).build()
                ));

        var score = service.calculateHealthScore(employerId);

        assertThat(score.riskLevel()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("calculateHealthScore includes all component scores")
    void calculateHealthScore_IncludesAllComponents() {
        when(endorsementRepository.countByEmployerIdAndStatus(eq(employerId), any()))
                .thenReturn(0L);
        when(anomalyDetectionRepository.countByEmployerIdAndFlaggedAtAfter(eq(employerId), any()))
                .thenReturn(0L);
        when(eaAccountRepository.findByEmployerId(employerId))
                .thenReturn(List.of());

        var score = service.calculateHealthScore(employerId);

        assertThat(score.employerId()).isEqualTo(employerId);
        assertThat(score.endorsementSuccessRate()).isNotNull();
        assertThat(score.anomalyScore()).isNotNull();
        assertThat(score.balanceHealthScore()).isNotNull();
        assertThat(score.reconciliationScore()).isNotNull();
        assertThat(score.calculatedAt()).isNotNull();
    }
}
