package com.plum.endorsements.application.service;

import com.plum.endorsements.api.dto.StpRateResponse;
import com.plum.endorsements.api.dto.StpRateTrendResponse;
import com.plum.endorsements.domain.model.*;
import com.plum.endorsements.domain.port.*;
import com.plum.endorsements.infrastructure.persistence.entity.EndorsementEventEntity;
import com.plum.endorsements.infrastructure.persistence.entity.EndorsementEntity;
import com.plum.endorsements.infrastructure.persistence.entity.InsurerConfigurationEntity;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataEndorsementEventRepository;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataEndorsementRepository;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataInsurerConfigurationRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessMiningService")
class ProcessMiningServiceTest {

    @Mock private ProcessMiningPort miningEngine;
    @Mock private ProcessMiningRepository miningRepository;
    @Mock private StpRateSnapshotRepository stpRateSnapshotRepository;
    @Mock private SpringDataEndorsementEventRepository eventRepository;
    @Mock private SpringDataEndorsementRepository endorsementRepository;
    @Mock private SpringDataInsurerConfigurationRepository insurerConfigRepo;
    @Mock private EventPublisher eventPublisher;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    @InjectMocks
    private ProcessMiningService service;

    private UUID insurerId;

    @BeforeEach
    void setUp() {
        insurerId = UUID.randomUUID();
    }

    @Test
    @DisplayName("analyzeInsurer fetches events, processes, and saves metrics")
    void analyzeInsurer_FetchesAndSavesMetrics() {
        UUID endorsementId = UUID.randomUUID();

        EndorsementEventEntity eventEntity = new EndorsementEventEntity();
        eventEntity.setEndorsementId(endorsementId);
        eventEntity.setEventType("ENDORSEMENT_CREATED");
        eventEntity.setCreatedAt(Instant.now());

        EndorsementEntity endEntity = new EndorsementEntity();
        endEntity.setId(endorsementId);
        endEntity.setInsurerId(insurerId);

        when(eventRepository.findAll()).thenReturn(List.of(eventEntity));
        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.of(endEntity));

        List<ProcessMiningMetric> metrics = List.of(
                ProcessMiningMetric.builder()
                        .insurerId(insurerId)
                        .fromStatus("CREATED")
                        .toStatus("VALIDATED")
                        .avgDurationMs(5000L)
                        .p95DurationMs(10000L)
                        .p99DurationMs(15000L)
                        .sampleCount(50)
                        .happyPathPct(new BigDecimal("85.00"))
                        .calculatedAt(Instant.now())
                        .build()
        );

        when(miningEngine.analyzeWorkflow(anyList(), eq(insurerId))).thenReturn(metrics);
        when(miningRepository.saveAll(metrics)).thenReturn(metrics);

        service.analyzeInsurer(insurerId);

        verify(miningRepository).deleteByInsurerId(insurerId);
        verify(miningRepository).saveAll(metrics);
    }

    @Test
    @DisplayName("getMetrics returns metrics for given insurer")
    void getMetrics_ReturnsMetricsForInsurer() {
        List<ProcessMiningMetric> expected = List.of(
                ProcessMiningMetric.builder()
                        .insurerId(insurerId)
                        .fromStatus("CREATED")
                        .toStatus("VALIDATED")
                        .avgDurationMs(3000L)
                        .sampleCount(100)
                        .build()
        );

        when(miningRepository.findByInsurerId(insurerId)).thenReturn(expected);

        List<ProcessMiningMetric> result = service.getMetrics(insurerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFromStatus()).isEqualTo("CREATED");
        assertThat(result.get(0).getSampleCount()).isEqualTo(100);
    }

    @Test
    @DisplayName("getMetrics with null insurerId returns all metrics from all insurers")
    void getMetrics_NullInsurerId_ReturnsAllMetrics() {
        UUID insurerId2 = UUID.randomUUID();

        InsurerConfigurationEntity config1 = new InsurerConfigurationEntity();
        config1.setInsurerId(insurerId);
        InsurerConfigurationEntity config2 = new InsurerConfigurationEntity();
        config2.setInsurerId(insurerId2);

        when(insurerConfigRepo.findAll()).thenReturn(List.of(config1, config2));
        when(miningRepository.findByInsurerId(insurerId)).thenReturn(List.of(
                ProcessMiningMetric.builder().insurerId(insurerId).fromStatus("A").toStatus("B").build()
        ));
        when(miningRepository.findByInsurerId(insurerId2)).thenReturn(List.of(
                ProcessMiningMetric.builder().insurerId(insurerId2).fromStatus("C").toStatus("D").build()
        ));

        List<ProcessMiningMetric> result = service.getMetrics(null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getStpRate returns STP rate for specific insurer")
    void getStpRate_SpecificInsurer_ReturnsRate() {
        ProcessMiningMetric metric = ProcessMiningMetric.builder()
                .insurerId(insurerId)
                .happyPathPct(new BigDecimal("92.50"))
                .build();

        when(miningRepository.findByInsurerId(insurerId)).thenReturn(List.of(metric));

        StpRateResponse response = service.getStpRate(insurerId);

        assertThat(response.overallStpRate()).isEqualByComparingTo(new BigDecimal("92.50"));
        assertThat(response.perInsurerStpRate()).containsKey(insurerId);
        assertThat(response.perInsurerStpRate().get(insurerId))
                .isEqualByComparingTo(new BigDecimal("92.50"));
    }

    @Test
    @DisplayName("getStpRate returns zero when no metrics exist")
    void getStpRate_NoMetrics_ReturnsZero() {
        when(miningRepository.findByInsurerId(insurerId)).thenReturn(List.of());

        StpRateResponse response = service.getStpRate(insurerId);

        assertThat(response.overallStpRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- Phase 3 Edge Case Tests ---

    @Test
    @DisplayName("analyzeInsurer handles no events for insurer gracefully")
    void shouldHandleNoEventsForInsurer() {
        // No events in the repository
        when(eventRepository.findAll()).thenReturn(List.of());
        when(miningEngine.analyzeWorkflow(anyList(), eq(insurerId))).thenReturn(List.of());
        when(miningRepository.saveAll(anyList())).thenReturn(List.of());

        service.analyzeInsurer(insurerId);

        verify(miningRepository).deleteByInsurerId(insurerId);
        verify(miningRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("analyzeInsurer filters out endorsements belonging to other insurers")
    void shouldFilterOutDeletedEndorsements() {
        UUID endorsementId = UUID.randomUUID();
        UUID otherInsurerId = UUID.randomUUID();

        EndorsementEventEntity eventEntity = new EndorsementEventEntity();
        eventEntity.setEndorsementId(endorsementId);
        eventEntity.setEventType("ENDORSEMENT_CREATED");
        eventEntity.setCreatedAt(Instant.now());

        // Endorsement belongs to a different insurer
        EndorsementEntity endEntity = new EndorsementEntity();
        endEntity.setId(endorsementId);
        endEntity.setInsurerId(otherInsurerId);

        when(eventRepository.findAll()).thenReturn(List.of(eventEntity));
        when(endorsementRepository.findById(endorsementId)).thenReturn(Optional.of(endEntity));
        // Empty list because all events are filtered out
        when(miningEngine.analyzeWorkflow(eq(List.of()), eq(insurerId))).thenReturn(List.of());
        when(miningRepository.saveAll(anyList())).thenReturn(List.of());

        service.analyzeInsurer(insurerId);

        verify(miningRepository).deleteByInsurerId(insurerId);
        // Should pass empty events since endorsement belongs to different insurer
        verify(miningEngine).analyzeWorkflow(argThat(List::isEmpty), eq(insurerId));
    }

    @Test
    @DisplayName("getLatestInsights returns bottleneck when p95 exceeds 2x avg and samples >= 5")
    void getLatestInsights_DetectsBottleneck_HighVariance() {
        InsurerConfigurationEntity config = new InsurerConfigurationEntity();
        config.setInsurerId(insurerId);
        config.setInsurerName("Test Insurer");

        when(insurerConfigRepo.findAll()).thenReturn(List.of(config));
        when(miningRepository.findByInsurerId(insurerId)).thenReturn(List.of(
                ProcessMiningMetric.builder()
                        .insurerId(insurerId)
                        .fromStatus("CREATED")
                        .toStatus("VALIDATED")
                        .avgDurationMs(3_600_000L)  // 1 hour avg
                        .p95DurationMs(10_800_000L) // 3 hours p95 (>2x avg)
                        .p99DurationMs(14_400_000L)
                        .sampleCount(10)
                        .calculatedAt(Instant.now())
                        .build()
        ));

        var insights = service.getLatestInsights();

        assertThat(insights).hasSize(1);
        assertThat(insights.get(0).insightType()).isEqualTo("BOTTLENECK");
        assertThat(insights.get(0).insight()).contains("CREATED");
        assertThat(insights.get(0).insight()).contains("VALIDATED");
        assertThat(insights.get(0).insurerName()).isEqualTo("Test Insurer");
    }

    @Test
    @DisplayName("getLatestInsights returns empty when no bottlenecks detected")
    void getLatestInsights_NoBottlenecks_ReturnsEmpty() {
        InsurerConfigurationEntity config = new InsurerConfigurationEntity();
        config.setInsurerId(insurerId);
        config.setInsurerName("Fast Insurer");

        when(insurerConfigRepo.findAll()).thenReturn(List.of(config));
        when(miningRepository.findByInsurerId(insurerId)).thenReturn(List.of(
                ProcessMiningMetric.builder()
                        .insurerId(insurerId)
                        .fromStatus("CREATED")
                        .toStatus("VALIDATED")
                        .avgDurationMs(5000L)    // 5 seconds avg
                        .p95DurationMs(8000L)    // 8 seconds p95 (< 2x avg)
                        .p99DurationMs(12000L)
                        .sampleCount(50)
                        .calculatedAt(Instant.now())
                        .build()
        ));

        var insights = service.getLatestInsights();

        assertThat(insights).isEmpty();
    }

    @Test
    @DisplayName("getLatestInsights skips metrics with insufficient samples (<5)")
    void getLatestInsights_SkipsLowSampleMetrics() {
        InsurerConfigurationEntity config = new InsurerConfigurationEntity();
        config.setInsurerId(insurerId);
        config.setInsurerName("Test Insurer");

        when(insurerConfigRepo.findAll()).thenReturn(List.of(config));
        when(miningRepository.findByInsurerId(insurerId)).thenReturn(List.of(
                ProcessMiningMetric.builder()
                        .insurerId(insurerId)
                        .fromStatus("CREATED")
                        .toStatus("VALIDATED")
                        .avgDurationMs(3_600_000L)
                        .p95DurationMs(10_800_000L)  // High variance but only 3 samples
                        .p99DurationMs(14_400_000L)
                        .sampleCount(3)
                        .calculatedAt(Instant.now())
                        .build()
        ));

        var insights = service.getLatestInsights();

        assertThat(insights).isEmpty();
    }

    @Test
    @DisplayName("getLatestInsights detects absolute bottleneck when avg > 4 hours")
    void getLatestInsights_DetectsAbsoluteBottleneck() {
        InsurerConfigurationEntity config = new InsurerConfigurationEntity();
        config.setInsurerId(insurerId);
        config.setInsurerName("Slow Insurer");

        when(insurerConfigRepo.findAll()).thenReturn(List.of(config));
        when(miningRepository.findByInsurerId(insurerId)).thenReturn(List.of(
                ProcessMiningMetric.builder()
                        .insurerId(insurerId)
                        .fromStatus("SUBMITTED_TO_INSURER")
                        .toStatus("CONFIRMED")
                        .avgDurationMs(18_000_000L)  // 5 hours avg (>4 hours threshold)
                        .p95DurationMs(20_000_000L)  // p95 NOT >2x avg, but absolute threshold triggers
                        .p99DurationMs(25_000_000L)
                        .sampleCount(20)
                        .calculatedAt(Instant.now())
                        .build()
        ));

        var insights = service.getLatestInsights();

        assertThat(insights).hasSize(1);
        assertThat(insights.get(0).insightType()).isEqualTo("BOTTLENECK");
        assertThat(insights.get(0).insight()).contains("SUBMITTED_TO_INSURER");
    }

    @Test
    @DisplayName("getStpRate falls back to status-based calculation when no metrics exist")
    void getStpRate_FallsBackToStatusCalculation() {
        when(miningRepository.findByInsurerId(insurerId)).thenReturn(List.of());
        // Simulate 8 confirmed, 2 rejected = 80% STP
        when(endorsementRepository.findByStatusAndInsurerId("CONFIRMED", insurerId))
                .thenReturn(Collections.nCopies(8, new com.plum.endorsements.infrastructure.persistence.entity.EndorsementEntity()));
        when(endorsementRepository.findByStatusAndInsurerId("REJECTED", insurerId))
                .thenReturn(Collections.nCopies(2, new com.plum.endorsements.infrastructure.persistence.entity.EndorsementEntity()));
        when(endorsementRepository.findByStatusAndInsurerId("FAILED", insurerId))
                .thenReturn(List.of());
        when(endorsementRepository.findByStatusAndInsurerId("FAILED_PERMANENT", insurerId))
                .thenReturn(List.of());

        StpRateResponse response = service.getStpRate(insurerId);

        assertThat(response.overallStpRate()).isEqualByComparingTo(new BigDecimal("80.0"));
    }

    @Test
    @DisplayName("getStpRate calculates correct STP rate with mixed outcomes across insurers")
    void shouldCalculateCorrectSTPRateWithMixedOutcomes() {
        UUID insurerId2 = UUID.randomUUID();

        InsurerConfigurationEntity config1 = new InsurerConfigurationEntity();
        config1.setInsurerId(insurerId);
        InsurerConfigurationEntity config2 = new InsurerConfigurationEntity();
        config2.setInsurerId(insurerId2);

        when(insurerConfigRepo.findAll()).thenReturn(List.of(config1, config2));

        // Insurer 1 has 90% STP rate
        when(miningRepository.findByInsurerId(insurerId)).thenReturn(List.of(
                ProcessMiningMetric.builder()
                        .insurerId(insurerId)
                        .happyPathPct(new BigDecimal("90.00"))
                        .build()
        ));

        // Insurer 2 has 80% STP rate
        when(miningRepository.findByInsurerId(insurerId2)).thenReturn(List.of(
                ProcessMiningMetric.builder()
                        .insurerId(insurerId2)
                        .happyPathPct(new BigDecimal("80.00"))
                        .build()
        ));

        StpRateResponse response = service.getStpRate(null);

        // Overall = (90 + 80) / 2 = 85.00
        assertThat(response.overallStpRate()).isEqualByComparingTo(new BigDecimal("85.00"));
        assertThat(response.perInsurerStpRate()).hasSize(2);
        assertThat(response.perInsurerStpRate().get(insurerId))
                .isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(response.perInsurerStpRate().get(insurerId2))
                .isEqualByComparingTo(new BigDecimal("80.00"));
    }

    // --- STP Rate Trending Tests ---

    @Test
    @DisplayName("captureStpRateSnapshot saves snapshot for valid insurer")
    void captureStpRateSnapshot_validInsurer_savesSnapshot() {
        when(endorsementRepository.findByStatusAndInsurerId("CONFIRMED", insurerId))
                .thenReturn(Collections.nCopies(8, new EndorsementEntity()));
        when(endorsementRepository.findByStatusAndInsurerId("REJECTED", insurerId))
                .thenReturn(Collections.nCopies(2, new EndorsementEntity()));
        when(endorsementRepository.findByStatusAndInsurerId("FAILED", insurerId))
                .thenReturn(List.of());
        when(endorsementRepository.findByStatusAndInsurerId("FAILED_PERMANENT", insurerId))
                .thenReturn(List.of());
        when(stpRateSnapshotRepository.save(any(StpRateSnapshot.class)))
                .thenAnswer(i -> i.getArgument(0));

        StpRateSnapshot result = service.captureStpRateSnapshot(insurerId);

        assertThat(result.getInsurerId()).isEqualTo(insurerId);
        assertThat(result.getTotalEndorsements()).isEqualTo(10);
        assertThat(result.getStpEndorsements()).isEqualTo(8);
        assertThat(result.getSnapshotDate()).isEqualTo(LocalDate.now());
        verify(stpRateSnapshotRepository).save(any(StpRateSnapshot.class));
    }

    @Test
    @DisplayName("captureStpRateSnapshot saves zero rate when no endorsements")
    void captureStpRateSnapshot_noEndorsements_savesZeroRate() {
        when(endorsementRepository.findByStatusAndInsurerId(anyString(), eq(insurerId)))
                .thenReturn(List.of());
        when(stpRateSnapshotRepository.save(any(StpRateSnapshot.class)))
                .thenAnswer(i -> i.getArgument(0));

        StpRateSnapshot result = service.captureStpRateSnapshot(insurerId);

        assertThat(result.getTotalEndorsements()).isZero();
        assertThat(result.getStpEndorsements()).isZero();
        assertThat(result.getStpRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getStpRateTrend returns ordered data points")
    void getStpRateTrend_multipleSnapshots_returnsOrderedByDate() {
        LocalDate today = LocalDate.now();
        List<StpRateSnapshot> snapshots = List.of(
                StpRateSnapshot.builder()
                        .insurerId(insurerId).snapshotDate(today.minusDays(2))
                        .totalEndorsements(10).stpEndorsements(7)
                        .stpRate(new BigDecimal("70.0000")).build(),
                StpRateSnapshot.builder()
                        .insurerId(insurerId).snapshotDate(today.minusDays(1))
                        .totalEndorsements(12).stpEndorsements(10)
                        .stpRate(new BigDecimal("83.3333")).build(),
                StpRateSnapshot.builder()
                        .insurerId(insurerId).snapshotDate(today)
                        .totalEndorsements(15).stpEndorsements(13)
                        .stpRate(new BigDecimal("86.6667")).build()
        );

        when(stpRateSnapshotRepository.findByInsurerIdAndDateRange(eq(insurerId), any(), any()))
                .thenReturn(snapshots);

        StpRateTrendResponse trend = service.getStpRateTrend(insurerId, 30);

        assertThat(trend.dataPoints()).hasSize(3);
        assertThat(trend.currentRate()).isEqualByComparingTo(new BigDecimal("86.6667"));
        assertThat(trend.changePercent()).isPositive();
        assertThat(trend.insurerId()).isEqualTo(insurerId);
    }

    @Test
    @DisplayName("getStpRateTrend returns empty data points when no snapshots exist")
    void getStpRateTrend_noData_returnsEmptyDataPoints() {
        when(stpRateSnapshotRepository.findByInsurerIdAndDateRange(eq(insurerId), any(), any()))
                .thenReturn(List.of());

        StpRateTrendResponse trend = service.getStpRateTrend(insurerId, 30);

        assertThat(trend.dataPoints()).isEmpty();
        assertThat(trend.currentRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(trend.changePercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
