package com.plum.endorsements.application.service;

import com.plum.endorsements.domain.model.InsurerConfiguration;
import com.plum.endorsements.domain.model.ProcessMiningMetric;
import com.plum.endorsements.domain.port.InsurerConfigurationRepository;
import com.plum.endorsements.domain.port.ProcessMiningRepository;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsurerBenchmarkService")
class InsurerBenchmarkServiceTest {

    @Mock private ProcessMiningRepository processMiningRepository;
    @Mock private InsurerConfigurationRepository insurerConfigurationRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MeterRegistry meterRegistry;

    @InjectMocks
    private InsurerBenchmarkService service;

    private UUID insurerId1;
    private UUID insurerId2;

    @BeforeEach
    void setUp() {
        insurerId1 = UUID.randomUUID();
        insurerId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("generateBenchmarks returns benchmarks for all active insurers")
    void generateBenchmarks_ReturnsAllActive() {
        InsurerConfiguration config1 = InsurerConfiguration.builder()
                .insurerId(insurerId1).insurerName("Insurer A").insurerCode("INS_A").build();
        InsurerConfiguration config2 = InsurerConfiguration.builder()
                .insurerId(insurerId2).insurerName("Insurer B").insurerCode("INS_B").build();

        when(insurerConfigurationRepository.findAllActive()).thenReturn(List.of(config1, config2));

        when(processMiningRepository.findByInsurerId(insurerId1)).thenReturn(List.of(
                ProcessMiningMetric.builder()
                        .insurerId(insurerId1).avgDurationMs(5000L).p95DurationMs(10000L)
                        .p99DurationMs(15000L).happyPathPct(new BigDecimal("90.0"))
                        .sampleCount(100).calculatedAt(Instant.now()).build()
        ));
        when(processMiningRepository.findByInsurerId(insurerId2)).thenReturn(List.of(
                ProcessMiningMetric.builder()
                        .insurerId(insurerId2).avgDurationMs(8000L).p95DurationMs(20000L)
                        .p99DurationMs(30000L).happyPathPct(new BigDecimal("75.0"))
                        .sampleCount(50).calculatedAt(Instant.now()).build()
        ));

        var benchmarks = service.generateBenchmarks();

        assertThat(benchmarks).hasSize(2);
        // Sorted by STP rate descending
        assertThat(benchmarks.get(0).insurerName()).isEqualTo("Insurer A");
        assertThat(benchmarks.get(0).stpRate()).isEqualByComparingTo(new BigDecimal("90.0"));
        assertThat(benchmarks.get(1).insurerName()).isEqualTo("Insurer B");
    }

    @Test
    @DisplayName("generateBenchmarks handles insurer with no metrics")
    void generateBenchmarks_NoMetrics_ReturnsZeros() {
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(insurerId1).insurerName("New Insurer").insurerCode("NEW").build();

        when(insurerConfigurationRepository.findAllActive()).thenReturn(List.of(config));
        when(processMiningRepository.findByInsurerId(insurerId1)).thenReturn(List.of());

        var benchmarks = service.generateBenchmarks();

        assertThat(benchmarks).hasSize(1);
        assertThat(benchmarks.get(0).avgProcessingMs()).isZero();
        assertThat(benchmarks.get(0).stpRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(benchmarks.get(0).totalSamples()).isZero();
    }

    @Test
    @DisplayName("generateBenchmarks returns empty list when no active insurers")
    void generateBenchmarks_NoInsurers_ReturnsEmpty() {
        when(insurerConfigurationRepository.findAllActive()).thenReturn(List.of());

        var benchmarks = service.generateBenchmarks();

        assertThat(benchmarks).isEmpty();
    }

    @Test
    @DisplayName("generateBenchmarks aggregates multiple metrics per insurer")
    void generateBenchmarks_MultipleMetrics_AggregatesCorrectly() {
        InsurerConfiguration config = InsurerConfiguration.builder()
                .insurerId(insurerId1).insurerName("Multi Metric").insurerCode("MM").build();

        when(insurerConfigurationRepository.findAllActive()).thenReturn(List.of(config));
        when(processMiningRepository.findByInsurerId(insurerId1)).thenReturn(List.of(
                ProcessMiningMetric.builder()
                        .avgDurationMs(4000L).p95DurationMs(8000L).p99DurationMs(12000L)
                        .happyPathPct(new BigDecimal("85.0")).sampleCount(50).build(),
                ProcessMiningMetric.builder()
                        .avgDurationMs(6000L).p95DurationMs(15000L).p99DurationMs(20000L)
                        .sampleCount(30).build()
        ));

        var benchmarks = service.generateBenchmarks();

        assertThat(benchmarks).hasSize(1);
        // Average of 4000 and 6000
        assertThat(benchmarks.get(0).avgProcessingMs()).isEqualTo(5000L);
        // Max p95
        assertThat(benchmarks.get(0).p95ProcessingMs()).isEqualTo(15000L);
        // Sum of samples
        assertThat(benchmarks.get(0).totalSamples()).isEqualTo(80);
    }
}
