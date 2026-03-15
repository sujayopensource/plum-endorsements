package com.plum.endorsements.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plum.endorsements.api.dto.ProcessMiningInsightResponse;
import com.plum.endorsements.api.dto.StpRateResponse;
import com.plum.endorsements.domain.model.EndorsementEvent;
import com.plum.endorsements.domain.model.EndorsementStatus;
import com.plum.endorsements.domain.model.ProcessMiningMetric;
import com.plum.endorsements.api.dto.StpRateTrendResponse;
import com.plum.endorsements.domain.model.StpRateSnapshot;
import com.plum.endorsements.domain.port.*;
import com.plum.endorsements.infrastructure.persistence.entity.EndorsementEventEntity;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataEndorsementEventRepository;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataEndorsementRepository;
import com.plum.endorsements.infrastructure.persistence.repository.SpringDataInsurerConfigurationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessMiningService {

    private final ProcessMiningPort miningEngine;
    private final ProcessMiningRepository miningRepository;
    private final StpRateSnapshotRepository stpRateSnapshotRepository;
    private final SpringDataEndorsementEventRepository eventRepository;
    private final SpringDataEndorsementRepository endorsementRepository;
    private final SpringDataInsurerConfigurationRepository insurerConfigRepo;
    private final EventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Transactional
    public void analyzeInsurer(UUID insurerId) {
        // Fetch all events for endorsements belonging to this insurer
        List<EndorsementEventEntity> eventEntities = eventRepository.findAll().stream()
                .filter(e -> {
                    // Check if the endorsement belongs to this insurer
                    return endorsementRepository.findById(e.getEndorsementId())
                            .map(end -> end.getInsurerId().equals(insurerId))
                            .orElse(false);
                })
                .toList();

        // Check if we have STATUS_CHANGE events with transition data
        List<EndorsementEventEntity> statusChangeEvents = eventEntities.stream()
                .filter(e -> "STATUS_CHANGE".equals(e.getEventType()))
                .toList();

        List<ProcessMiningMetric> metrics;

        if (!statusChangeEvents.isEmpty()) {
            // Parse transition data directly from STATUS_CHANGE events
            metrics = analyzeFromStatusChangeEvents(statusChangeEvents, insurerId);
        } else {
            // Fallback: use EventStreamAnalyzer for generic events
            List<EndorsementEvent> events = eventEntities.stream()
                    .map(e -> (EndorsementEvent) new EndorsementEvent.Created(
                            e.getEndorsementId(), e.getCreatedAt(),
                            UUID.randomUUID(), UUID.randomUUID(),
                            com.plum.endorsements.domain.model.EndorsementType.ADD))
                    .toList();
            metrics = miningEngine.analyzeWorkflow(events, insurerId);
        }

        // Clear old metrics for this insurer and save new ones
        miningRepository.deleteByInsurerId(insurerId);
        miningRepository.saveAll(metrics);

        // Update Micrometer gauges
        metrics.stream()
                .filter(m -> m.getHappyPathPct() != null)
                .findFirst()
                .ifPresent(m -> {
                    meterRegistry.gauge("endorsement.process.stp_rate",
                            io.micrometer.core.instrument.Tags.of("insurerId", insurerId.toString()),
                            m.getHappyPathPct().doubleValue());
                    meterRegistry.gauge("endorsement.process.avg_lifecycle_hours",
                            io.micrometer.core.instrument.Tags.of("insurerId", insurerId.toString()),
                            m.getAvgDurationMs() / 3_600_000.0);
                });

        log.info("Process mining completed for insurer {}: {} transition metrics saved",
                insurerId, metrics.size());
    }

    private List<ProcessMiningMetric> analyzeFromStatusChangeEvents(
            List<EndorsementEventEntity> statusChangeEvents, UUID insurerId) {

        Map<String, DescriptiveStatistics> transitionStats = new HashMap<>();
        Map<UUID, Boolean> endorsementHappyPath = new HashMap<>();

        for (EndorsementEventEntity event : statusChangeEvents) {
            try {
                JsonNode data = OBJECT_MAPPER.readTree(event.getEventData());
                String statusFrom = data.has("statusFrom") ? data.get("statusFrom").asText() : "";
                String statusTo = data.has("statusTo") ? data.get("statusTo").asText() : "";
                int durationMinutes = data.has("durationMinutes") ? data.get("durationMinutes").asInt() : 0;

                String key = statusFrom + " -> " + statusTo;
                long durationMs = durationMinutes * 60_000L;
                transitionStats.computeIfAbsent(key, k -> new DescriptiveStatistics()).addValue(durationMs);

                // Track happy path: any REJECTED transition means deviated
                if ("REJECTED".equals(statusTo) || "REJECTED".equals(statusFrom)) {
                    endorsementHappyPath.put(event.getEndorsementId(), false);
                } else {
                    endorsementHappyPath.putIfAbsent(event.getEndorsementId(), true);
                }
            } catch (Exception e) {
                log.warn("Failed to parse event data for event {}: {}", event.getId(), e.getMessage());
            }
        }

        int totalEndorsements = endorsementHappyPath.size();
        long happyPathCount = endorsementHappyPath.values().stream().filter(Boolean::booleanValue).count();
        BigDecimal happyPathPct = totalEndorsements > 0
                ? BigDecimal.valueOf(happyPathCount * 100.0 / totalEndorsements)
                    .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<ProcessMiningMetric> metrics = new ArrayList<>();
        Instant now = Instant.now();

        for (var entry : transitionStats.entrySet()) {
            String[] parts = entry.getKey().split(" -> ");
            DescriptiveStatistics stats = entry.getValue();

            ProcessMiningMetric metric = ProcessMiningMetric.builder()
                    .insurerId(insurerId)
                    .fromStatus(parts[0])
                    .toStatus(parts[1])
                    .avgDurationMs((long) stats.getMean())
                    .p95DurationMs((long) stats.getPercentile(95))
                    .p99DurationMs((long) stats.getPercentile(99))
                    .sampleCount((int) stats.getN())
                    .happyPathPct(happyPathPct)
                    .calculatedAt(now)
                    .build();

            metrics.add(metric);
        }

        return metrics;
    }

    @Transactional
    public void generateInsights() {
        insurerConfigRepo.findAll().forEach(config -> {
            try {
                analyzeInsurer(config.getInsurerId());
            } catch (Exception e) {
                log.error("Process mining failed for insurer {}: {}", config.getInsurerId(), e.getMessage());
            }
        });
    }

    public List<ProcessMiningMetric> getMetrics(UUID insurerId) {
        if (insurerId != null) {
            return miningRepository.findByInsurerId(insurerId);
        }
        // Return all metrics from all insurers
        List<ProcessMiningMetric> all = new ArrayList<>();
        insurerConfigRepo.findAll().forEach(config ->
                all.addAll(miningRepository.findByInsurerId(config.getInsurerId())));
        return all;
    }

    public List<ProcessMiningInsightResponse> getLatestInsights() {
        List<ProcessMiningInsightResponse> insights = new ArrayList<>();

        insurerConfigRepo.findAll().forEach(config -> {
            UUID insurerId = config.getInsurerId();
            List<ProcessMiningMetric> metrics = miningRepository.findByInsurerId(insurerId);

            // Identify bottlenecks (p95 > 2x average, or avg duration > 4 hours)
            for (ProcessMiningMetric metric : metrics) {
                boolean isHighVariance = metric.getP95DurationMs() > metric.getAvgDurationMs() * 2;
                boolean isAbsolutelySlowTransition = metric.getAvgDurationMs() > 4 * 3_600_000L;
                if ((isHighVariance || isAbsolutelySlowTransition) && metric.getSampleCount() >= 5) {
                    insights.add(new ProcessMiningInsightResponse(
                            insurerId, config.getInsurerName(),
                            "BOTTLENECK",
                            String.format("Bottleneck detected: %s → %s averages %.1f hours (p95: %.1f hours). " +
                                    "Based on %d samples.",
                                    metric.getFromStatus(), metric.getToStatus(),
                                    metric.getAvgDurationMs() / 3_600_000.0,
                                    metric.getP95DurationMs() / 3_600_000.0,
                                    metric.getSampleCount()),
                            metric.getCalculatedAt()
                    ));
                }
            }
        });

        return insights;
    }

    public StpRateResponse getStpRate(UUID insurerId) {
        if (insurerId != null) {
            List<ProcessMiningMetric> metrics = miningRepository.findByInsurerId(insurerId);
            BigDecimal stpRate = metrics.stream()
                    .filter(m -> m.getHappyPathPct() != null)
                    .findFirst()
                    .map(ProcessMiningMetric::getHappyPathPct)
                    .orElseGet(() -> computeStpFromStatuses(insurerId));

            long[] counts = countEndorsements(insurerId);
            return new StpRateResponse(stpRate, Map.of(insurerId, stpRate), counts[0], counts[1]);
        }

        // Aggregate across all insurers
        Map<UUID, BigDecimal> perInsurer = new LinkedHashMap<>();
        long totalAll = 0;
        long successfulAll = 0;

        // Include configured insurers
        Set<UUID> processedInsurers = new HashSet<>();
        for (var config : insurerConfigRepo.findAll()) {
            UUID insId = config.getInsurerId();
            processedInsurers.add(insId);
            List<ProcessMiningMetric> metrics = miningRepository.findByInsurerId(insId);
            BigDecimal rate = metrics.stream()
                    .filter(m -> m.getHappyPathPct() != null)
                    .findFirst()
                    .map(ProcessMiningMetric::getHappyPathPct)
                    .orElseGet(() -> computeStpFromStatuses(insId));
            perInsurer.put(insId, rate);
            long[] counts = countEndorsements(insId);
            totalAll += counts[0];
            successfulAll += counts[1];
        }

        // Also include unconfigured insurers that have endorsements
        List<String> terminalStatuses = List.of("CONFIRMED", "REJECTED", "FAILED", "FAILED_PERMANENT");
        for (String status : terminalStatuses) {
            endorsementRepository.findByStatus(status).forEach(e -> {
                if (!processedInsurers.contains(e.getInsurerId())) {
                    processedInsurers.add(e.getInsurerId());
                    BigDecimal rate = computeStpFromStatuses(e.getInsurerId());
                    perInsurer.put(e.getInsurerId(), rate);
                }
            });
        }

        // Recompute totals from perInsurer
        totalAll = 0;
        successfulAll = 0;
        for (UUID insId : perInsurer.keySet()) {
            long[] counts = countEndorsements(insId);
            totalAll += counts[0];
            successfulAll += counts[1];
        }

        BigDecimal overall;
        if (!perInsurer.isEmpty()) {
            overall = perInsurer.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(perInsurer.size()), 2, RoundingMode.HALF_UP);
        } else {
            overall = BigDecimal.ZERO;
        }

        return new StpRateResponse(overall, perInsurer, totalAll, successfulAll);
    }

    private BigDecimal computeStpFromStatuses(UUID insurerId) {
        long confirmed = endorsementRepository.findByStatusAndInsurerId("CONFIRMED", insurerId).size();
        long rejected = endorsementRepository.findByStatusAndInsurerId("REJECTED", insurerId).size();
        long failed = endorsementRepository.findByStatusAndInsurerId("FAILED", insurerId).size()
                + endorsementRepository.findByStatusAndInsurerId("FAILED_PERMANENT", insurerId).size();
        long total = confirmed + rejected + failed;
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(confirmed * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }

    private long[] countEndorsements(UUID insurerId) {
        long confirmed = endorsementRepository.findByStatusAndInsurerId("CONFIRMED", insurerId).size();
        long rejected = endorsementRepository.findByStatusAndInsurerId("REJECTED", insurerId).size();
        long failed = endorsementRepository.findByStatusAndInsurerId("FAILED", insurerId).size()
                + endorsementRepository.findByStatusAndInsurerId("FAILED_PERMANENT", insurerId).size();
        return new long[]{confirmed + rejected + failed, confirmed};
    }

    @Transactional
    public StpRateSnapshot captureStpRateSnapshot(UUID insurerId) {
        long[] counts = countEndorsements(insurerId);
        long total = counts[0];
        long stp = counts[1];
        BigDecimal rate = total > 0
                ? BigDecimal.valueOf(stp * 100.0 / total).setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        StpRateSnapshot snapshot = StpRateSnapshot.builder()
                .insurerId(insurerId)
                .snapshotDate(LocalDate.now())
                .totalEndorsements((int) total)
                .stpEndorsements((int) stp)
                .stpRate(rate)
                .createdAt(Instant.now())
                .build();

        return stpRateSnapshotRepository.save(snapshot);
    }

    @Transactional
    public void captureAllStpRateSnapshots() {
        insurerConfigRepo.findAll().forEach(config -> {
            try {
                captureStpRateSnapshot(config.getInsurerId());
            } catch (Exception e) {
                log.error("STP rate snapshot capture failed for insurer {}: {}",
                        config.getInsurerId(), e.getMessage());
            }
        });
    }

    @Transactional(readOnly = true)
    public StpRateTrendResponse getStpRateTrend(UUID insurerId, int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days);

        List<StpRateSnapshot> snapshots = stpRateSnapshotRepository
                .findByInsurerIdAndDateRange(insurerId, from, to);

        List<StpRateTrendResponse.DataPoint> dataPoints = snapshots.stream()
                .map(s -> new StpRateTrendResponse.DataPoint(
                        s.getSnapshotDate(), s.getStpRate(),
                        s.getTotalEndorsements(), s.getStpEndorsements()))
                .toList();

        BigDecimal currentRate = dataPoints.isEmpty()
                ? BigDecimal.ZERO
                : dataPoints.get(dataPoints.size() - 1).stpRate();

        BigDecimal changePercent = BigDecimal.ZERO;
        if (dataPoints.size() >= 2) {
            BigDecimal first = dataPoints.get(0).stpRate();
            BigDecimal last = dataPoints.get(dataPoints.size() - 1).stpRate();
            changePercent = last.subtract(first);
        }

        return new StpRateTrendResponse(insurerId, dataPoints, currentRate, changePercent);
    }
}
