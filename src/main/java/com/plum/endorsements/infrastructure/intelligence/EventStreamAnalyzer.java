package com.plum.endorsements.infrastructure.intelligence;

import com.plum.endorsements.domain.model.EndorsementEvent;
import com.plum.endorsements.domain.model.ProcessMiningMetric;
import com.plum.endorsements.domain.port.ProcessMiningPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EventStreamAnalyzer implements ProcessMiningPort {

    @Override
    public List<ProcessMiningMetric> analyzeWorkflow(List<EndorsementEvent> events, UUID insurerId) {
        if (events.isEmpty()) {
            return List.of();
        }

        // Group events by endorsementId
        Map<UUID, List<EndorsementEvent>> byEndorsement = events.stream()
                .collect(Collectors.groupingBy(EndorsementEvent::endorsementId));

        // Calculate transitions
        Map<String, DescriptiveStatistics> transitionStats = new HashMap<>();
        int totalEndorsements = byEndorsement.size();
        int happyPathCount = 0;

        for (var entry : byEndorsement.entrySet()) {
            List<EndorsementEvent> timeline = entry.getValue().stream()
                    .sorted(Comparator.comparing(EndorsementEvent::occurredAt))
                    .toList();

            boolean isHappyPath = true;

            for (int i = 0; i < timeline.size() - 1; i++) {
                EndorsementEvent from = timeline.get(i);
                EndorsementEvent to = timeline.get(i + 1);

                String transitionKey = from.eventType() + " -> " + to.eventType();
                long durationMs = Duration.between(from.occurredAt(), to.occurredAt()).toMillis();

                transitionStats.computeIfAbsent(transitionKey, k -> new DescriptiveStatistics())
                        .addValue(durationMs);

                // Check if endorsement hit retry
                if (to.eventType().contains("RETRY") || to.eventType().contains("REJECTED")) {
                    isHappyPath = false;
                }
            }

            if (isHappyPath) happyPathCount++;
        }

        BigDecimal happyPathPct = totalEndorsements > 0
                ? BigDecimal.valueOf(happyPathCount).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalEndorsements), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Build metrics
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

        log.info("Process mining for insurer {}: {} transitions analyzed, {}% happy path, {} total endorsements",
                insurerId, transitionStats.size(), happyPathPct, totalEndorsements);

        return metrics;
    }
}
