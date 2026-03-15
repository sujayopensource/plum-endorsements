package com.plum.endorsements.application.service;

import com.plum.endorsements.domain.model.InsurerConfiguration;
import com.plum.endorsements.domain.model.ProcessMiningMetric;
import com.plum.endorsements.domain.port.InsurerConfigurationRepository;
import com.plum.endorsements.domain.port.ProcessMiningRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsurerBenchmarkService {

    private final ProcessMiningRepository processMiningRepository;
    private final InsurerConfigurationRepository insurerConfigurationRepository;
    private final MeterRegistry meterRegistry;

    public List<InsurerBenchmark> generateBenchmarks() {
        log.info("Generating cross-insurer benchmarks");

        List<InsurerConfiguration> insurers = insurerConfigurationRepository.findAllActive();
        List<InsurerBenchmark> benchmarks = new ArrayList<>();

        for (InsurerConfiguration insurer : insurers) {
            List<ProcessMiningMetric> metrics = processMiningRepository.findByInsurerId(insurer.getInsurerId());

            if (metrics.isEmpty()) {
                benchmarks.add(new InsurerBenchmark(
                        insurer.getInsurerId(), insurer.getInsurerName(), insurer.getInsurerCode(),
                        0L, 0L, 0L, BigDecimal.ZERO, 0, Instant.now()
                ));
                continue;
            }

            long avgProcessingMs = (long) metrics.stream()
                    .mapToLong(ProcessMiningMetric::getAvgDurationMs)
                    .average().orElse(0);

            long p95ProcessingMs = metrics.stream()
                    .mapToLong(ProcessMiningMetric::getP95DurationMs)
                    .max().orElse(0);

            long p99ProcessingMs = metrics.stream()
                    .mapToLong(ProcessMiningMetric::getP99DurationMs)
                    .max().orElse(0);

            BigDecimal stpRate = metrics.stream()
                    .filter(m -> m.getHappyPathPct() != null)
                    .findFirst()
                    .map(ProcessMiningMetric::getHappyPathPct)
                    .orElse(BigDecimal.ZERO);

            int totalSamples = metrics.stream()
                    .mapToInt(ProcessMiningMetric::getSampleCount)
                    .sum();

            benchmarks.add(new InsurerBenchmark(
                    insurer.getInsurerId(), insurer.getInsurerName(), insurer.getInsurerCode(),
                    avgProcessingMs, p95ProcessingMs, p99ProcessingMs,
                    stpRate, totalSamples, Instant.now()
            ));
        }

        // Sort by STP rate descending (best performers first)
        benchmarks.sort((a, b) -> b.stpRate().compareTo(a.stpRate()));

        log.info("Generated benchmarks for {} insurers", benchmarks.size());
        return benchmarks;
    }

    public record InsurerBenchmark(
            UUID insurerId,
            String insurerName,
            String insurerCode,
            long avgProcessingMs,
            long p95ProcessingMs,
            long p99ProcessingMs,
            BigDecimal stpRate,
            int totalSamples,
            Instant calculatedAt
    ) {}
}
